package com.designpattern.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * @author MSA
 */

public class DbCustomerDao implements CustomerDao {
	private static final Logger LOGGER = Logger.getLogger(DbCustomerDao.class);

	private final DataSource dataSource;

	public DbCustomerDao(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public Stream<Customer> getAll() throws Exception {
		Connection connection;
		try {
			connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM CUSTOMERS"); // NOSONAR
			ResultSet resultSet = statement.executeQuery(); // NOSONAR
			
			return StreamSupport
					.stream(new Spliterators.AbstractSpliterator<Customer>(Long.MAX_VALUE, Spliterator.ORDERED) {
						public boolean tryAdvance(Consumer<? super Customer> action) {
							try {
								if (!resultSet.next()) {
									return false;
								}
								
								action.accept(createCustomer(resultSet));
								return true;
							} catch (SQLException e) {
								throw new RuntimeException(e); // NOSONAR
							}
						}
					}, false).onClose(() -> mutedClose(connection, statement, resultSet));
		} catch (SQLException e) {
			throw new CustomException(e.getMessage(), e);
		}
	}

	private Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	private void mutedClose(Connection connection, PreparedStatement statement, ResultSet resultSet) {
		try {
			resultSet.close();
			statement.close();
			connection.close();
		} catch (SQLException e) {
			LOGGER.info("Exception thrown " + e.getMessage());
		}
	}

	private Customer createCustomer(ResultSet resultSet) throws SQLException {
		return new Customer(resultSet.getInt("ID"), resultSet.getString("FNAME"), resultSet.getString("LNAME"));
	}

	public Optional<Customer> getById(int id) throws Exception {
		ResultSet resultSet = null;

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM CUSTOMERS WHERE ID = ?")) {

			statement.setInt(1, id);
			resultSet = statement.executeQuery();

			if (resultSet.next()) {
				return Optional.of(createCustomer(resultSet));
			} else {
				return Optional.empty();
			}
		} catch (SQLException ex) {
			throw new CustomException(ex.getMessage(), ex);
		} finally {
			if (resultSet != null) {
				resultSet.close();
			}
		}
	}

	public boolean add(Customer customer) throws Exception {
		if (getById(customer.getId()).isPresent()) {
			return false;
		}

		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("INSERT INTO CUSTOMERS VALUES (?,?,?)")) {
			statement.setInt(1, customer.getId());
			statement.setString(2, customer.getFirstName());
			statement.setString(3, customer.getLastName());
			statement.execute();
			return true;
		} catch (SQLException ex) {
			throw new CustomException(ex.getMessage(), ex);
		}
	}

	public boolean update(Customer customer) throws Exception {
		try (Connection connection = getConnection();
				PreparedStatement statement = connection
						.prepareStatement("UPDATE CUSTOMERS SET FNAME = ?, LNAME = ? WHERE ID = ?")) {
			statement.setString(1, customer.getFirstName());
			statement.setString(2, customer.getLastName());
			statement.setInt(3, customer.getId());
			return statement.executeUpdate() > 0;
		} catch (SQLException ex) {
			throw new CustomException(ex.getMessage(), ex);
		}
	}

	public boolean delete(Customer customer) throws Exception {
		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM CUSTOMERS WHERE ID = ?")) {
			statement.setInt(1, customer.getId());
			return statement.executeUpdate() > 0;
		} catch (SQLException ex) {
			throw new CustomException(ex.getMessage(), ex);
		}
	}
}