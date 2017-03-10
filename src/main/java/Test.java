import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.input.image.ClarifaiImage;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

public class Test {
	// creat class for storing url, name and value
	final static int SIZE_OF_IMAGE_URL = 3;
	
	public static class Node{
		private String url;
		private String name;
		private float value;
		Node(String u, String n, float v){
			this.url = u;
			this.name = n;
			this.value = v;
		}
		public String getName(){
			return this.name;
		}
		public String getUrl(){
			return this.url;
		}
		public float getValue(){
			return this.value;
		}
	}
	// read file of urls from given txt file and output as list of String
	public static List<String> readImages(ClarifaiClient client) throws IOException{
		List<String> images = new ArrayList<String>();
		
		BufferedReader br = new BufferedReader(new FileReader("/Users/outianleiz/Downloads/images_test.txt"));
		try {		    
		    String line = br.readLine();
		    while (line != null) {
		        images.add(line);
		        line = br.readLine();
		    }
		} finally {
		    br.close();
		}
		 return images;   
	}
	// get data and store as a Hashmap in memory to help searching
	public static HashMap<String, PriorityQueue<Node>> tagImages(ClarifaiClient client) throws IOException {
		List<String> images = readImages(client);       				// read urls from txt file;
		System.out.println("The number of total input images are " + images.size());
		HashMap<String, PriorityQueue<Node>> map = new HashMap<>();
		Comparator<Node> cmp = new Comparator<Node>(){                       // use for PriorityQueue for creating min heap
			public int compare(Node a, Node b){
				float n = a.value - b.value;
				return n < 0 ? -1:1;
			}
		};
		
		for(int i = 0; i < Math.ceil(images.size() / (SIZE_OF_IMAGE_URL * 1.0)); i++){
			List<ClarifaiInput> inputSet = new ArrayList<ClarifaiInput>();	
			int len = Math.min(SIZE_OF_IMAGE_URL, images.size() - (i * SIZE_OF_IMAGE_URL));
			System.out.print("Packing the index of images : ");
			for(int j = 0; j < len; j++){
				int index = j + i * SIZE_OF_IMAGE_URL;
				System.out.print(index + ",");
				inputSet.add(ClarifaiInput.forImage(ClarifaiImage.of(images.get(index))));
			}
			System.out.println();
			System.out.println("Size of images send is " + inputSet.size());
			List<ClarifaiOutput<Concept>> predictionResults =
			    client.getDefaultModels().generalModel() // You can also do Clarifai.getModelByID("id") to get custom models
			        .predict()
			        .withInputs(inputSet)
			        .executeSync() // optionally, pass a ClarifaiClient parameter to override the default client instance with another one
			        .get();
			for(int k = 0; k < predictionResults.size(); k++){
				for(int l = 0; l < predictionResults.get(k).data().size(); l++){
					String name = predictionResults.get(k).data().get(l).name();
					float value = predictionResults.get(k).data().get(l).value();
					String url = images.get(k + i * SIZE_OF_IMAGE_URL);
					Node node = new Node(url, name, value);
					
					if(!map.containsKey(name)){
						map.put(name, new PriorityQueue<Node>(10, cmp));
					}
					if(map.get(name).size() >= 10 && value > map.get(name).peek().getValue()){
						map.get(name).poll();
					}
					if(map.get(name).size() < 10){
						map.get(name).add(node);
					}
				}

			}
		}
		return map;
		
	}
	
	public static void main(String[] args) throws IOException {
		
		String ID = "zAxfuT0YtzPmJyRBxM0e1zP2gyDJbdPfn01u45mg";
		String Secret = "SA1IuiJjh5ym1qVmyWwRczm3Y0CGvNFCFjT5eaBP";
		final ClarifaiClient client = new ClarifaiBuilder(ID, Secret).buildSync();
		
		HashMap<String, PriorityQueue<Node>> map = tagImages(client);
		System.out.println("Pick tag from: ");
		for(String key : map.keySet()){
			System.out.print(key + ",");
		}
		System.out.println();
		System.out.println("Enter tag you want or type '!' to exit: ");
		
		 Scanner scan = new Scanner(System.in);
	        try {
	            while (scan.hasNext() ){
	                String line = scan.nextLine().toLowerCase();
	                if(map.containsKey(line)){
	                	for(Node node : map.get(line)){
	                		System.out.println(node.getUrl());
	                	}  
	                }else if(line.equals("!")){
	                	break;
	                }else{
	                	System.out.println("Tag is not exist, plaease try another one : ");
	                }
	           
	            }
	        } finally {
	        	System.out.println("System End");
	            scan.close();
	        }
	}
}
