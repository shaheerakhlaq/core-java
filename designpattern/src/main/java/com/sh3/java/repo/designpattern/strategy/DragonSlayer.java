package com.sh3.java.repo.designpattern.strategy;

/**
 * @author MSA
 */

public class DragonSlayer {
	private DragonSlayingStrategy strategy;

	public DragonSlayer(DragonSlayingStrategy strategy) {
		this.strategy = strategy;
	}

	public void changeStrategy(DragonSlayingStrategy strategy) {
		this.strategy = strategy;
	}

	public void goToBattle() {
		strategy.execute();
	}
}