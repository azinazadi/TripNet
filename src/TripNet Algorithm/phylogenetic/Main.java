package phylogenetic;



import phylogenetic.*;


public class Main {
	public static void main(String args[]){
		TripNet.speed=TripNet.SPEED_SLOW;
//		TripNet.REAMOVE_CYCLIC_TRIPLETS = false;
		TripNet.NORMAL_SPEED_NUM_OF_CHECKED_RETICS = 100;
		
		TripNet.main(args);
	}
}
