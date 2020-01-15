package processor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class OrdersProcessor implements Runnable {
	private int numOfOrders, current;
	private String dataLocation, fileBase, resultsLocation, multiThread;
	private ArrayList<Thread> multiThreadArray = new ArrayList<Thread>();

	private Map<String, Double> costsMap = new TreeMap<String, Double>();
	private Map<String, Double> ordersMap = new TreeMap<String, Double>();

	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner userInput = new Scanner(System.in);

		OrdersProcessor ordersProcessor = new OrdersProcessor();

		System.out.print("Enter item's data file name: ");
		ordersProcessor.dataLocation = userInput.nextLine();

		System.out.print("Enter 'y' for multiple threads, any other character otherwise: ");
		ordersProcessor.multiThread = userInput.nextLine();

		System.out.print("Enter number of orders to process: ");
		ordersProcessor.numOfOrders = userInput.nextInt();
		userInput.nextLine();

		System.out.print("Enter order's base filename: ");
		ordersProcessor.fileBase = userInput.nextLine();

		System.out.print("Enter result's filename: ");
		ordersProcessor.resultsLocation = userInput.nextLine();

		if (ordersProcessor.multiThread.equals("y")) {
			ordersProcessor.multiThread();
		} else {
			ordersProcessor.singleThread();
		}
	}

	public void multiThread() {
		long startTime = System.currentTimeMillis();

		current = 1;

		for (int i = 1; i <= this.numOfOrders; i++) {
			Thread t = new Thread(this);

			multiThreadArray.add(t);

			t.start();
		}

		try {
			for (Thread thread : multiThreadArray) {
				thread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		this.summary(this.ordersMap);

		long endTime = System.currentTimeMillis();

		System.out.println("Processing time (msec): " + (endTime - startTime));
	}

	public void singleThread() {
		long startTime = System.currentTimeMillis();

		Thread t = new Thread(this);

		current = 1;

		for (int i = 1; i <= this.numOfOrders; i++) {
			// t.run();
			t = new Thread(this);
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		this.summary(this.ordersMap);

		long endTime = System.currentTimeMillis();

		System.out.println("Processing time (msec): " + (endTime - startTime));
	}

	public void setData() {
		try {
			String data = null;

			File file = new File(dataLocation);

			FileInputStream fileInputStream = new FileInputStream(file);

			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

			@SuppressWarnings("resource")
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			while ((data = bufferedReader.readLine()) != null) {
				String itemName = data.substring(0, data.indexOf(" "));

				Double itemRate = Double.parseDouble(data.substring(data.indexOf(" ") + 1));

				costsMap.put(itemName, itemRate);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeToFile(String str) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(resultsLocation, true));

			writer.append(str + "\n");

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void summary(Map<String, Double> map) {
		writeToFile("***** Summary of all orders *****");

		double grandTotal = 0;

		for (String item : map.keySet()) {
			writeToFile("Summary - Item's name: " + item + ", Cost per item: "
					+ NumberFormat.getCurrencyInstance().format(costsMap.get(item)) + ", Number sold: "
					+ Math.round(map.get(item)) + ", Item's Total: "
					+ NumberFormat.getCurrencyInstance().format(map.get(item) * costsMap.get(item)));

			grandTotal += map.get(item) * costsMap.get(item);
		}

		writeToFile("Summary Grand Total: " + NumberFormat.getCurrencyInstance().format(grandTotal));
	}

	public void order(Map<String, Double> map, String clientId) {
		writeToFile("----- Order details for client with Id: " + clientId + " -----");

		double totalCost = 0;

		for (String item : map.keySet()) {
			writeToFile("Item's name: " + item + ", Cost per item: "
					+ NumberFormat.getCurrencyInstance().format(costsMap.get(item)) + ", Quantity: "
					+ Math.round(map.get(item)) + ", Cost: "
					+ NumberFormat.getCurrencyInstance().format(map.get(item) * costsMap.get(item)));

			totalCost += map.get(item) * costsMap.get(item);
		}

		writeToFile("Order Total: " + NumberFormat.getCurrencyInstance().format(totalCost));
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		setData();

		String line = null;

		synchronized (this) {
			File file = new File(fileBase + current + ".txt");

			Map<String, Double> singleOrdersMap = new TreeMap<String, Double>();

			try {
				FileInputStream fileInputStream = new FileInputStream(file);

				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

				@SuppressWarnings("resource")
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String clientId = bufferedReader.readLine().substring(10);

				System.out.println("Reading order for client with id: " + clientId);

				while ((line = bufferedReader.readLine()) != null) {
					String itemName = line.substring(0, line.indexOf(" "));

					if (singleOrdersMap.containsKey(itemName)) {
						singleOrdersMap.put(itemName, singleOrdersMap.get(itemName) + 1);
					} else {
						singleOrdersMap.put(itemName, new Double(1));
					}

					if (ordersMap.containsKey(itemName)) {
						ordersMap.put(itemName, ordersMap.get(itemName) + 1);
					} else {
						ordersMap.put(itemName, new Double(1));
					}
				}

				order(singleOrdersMap, clientId);

				current++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
