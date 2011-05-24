package phylogenetic;

abstract class ConsistencyCheckListener
{
  boolean isNetworkChanged = false;

  public abstract boolean inconsistentTripletFound(Triplet paramTriplet);
}