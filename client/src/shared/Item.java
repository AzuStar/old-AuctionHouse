package shared;

import java.io.Serializable;

public class Item implements Serializable {

	public int id;
	public String name = "";
	public String desc = "";
	private double reserve_price = 0;
	public String bidder = "";
	public double bid = 0;

	public Item(String name, String desc, double start_price, double reserve_price) {
		this.name = name;
		this.desc = desc;
		bid = start_price;
		this.reserve_price = reserve_price;
	}

}