package edu.cmu.ml.proppr.graph;



import java.util.HashMap;

public abstract class LearningGraphBuilder {
	public static final char TAB = '\t';
	public static final char FEATURE_INDEX_DELIM = ':';
	public static final String SRC_DST_DELIM = "->";
	public static final char EDGE_DELIM = ':';
	public static final char EDGE_FEATURE_DELIM = ',';
	public static final char FEATURE_WEIGHT_DELIM = '@';

	public abstract LearningGraphBuilder copy();
	public abstract LearningGraph create();
	public abstract void setGraphSize(LearningGraph g, int nodeSize, int edgeSize, int labelDependencySize);
	public abstract void addOutlink(LearningGraph g, int u, RWOutlink rwOutlink);
	public abstract void freeze(LearningGraph g);
	public abstract void index(int i0);
	public LearningGraph deserialize(String string) throws GraphFormatException {
		// first parse the graph metadata
		String[] parts = new String[4];
		int last = 0,i=0;
		for (int next = last; i<parts.length; last=next+1,i++) {
			if (next == -1) 
				throw new GraphFormatException("Need 5 distinct tsv fields in the graph:"+string);
			next=string.indexOf(TAB,last);
			parts[i] = next<0?string.substring(last):string.substring(last,next);
		}

		// graph metadata is
		// #nodes
		// #edges
		// #label dependencies

		int nodeSize = Integer.parseInt(parts[0]);
		int edgeSize = Integer.parseInt(parts[1]);
		int dependencySize = Integer.parseInt(parts[2]);
		ArrayLearningGraphBuilder b = new ArrayLearningGraphBuilder();
		LearningGraph g = b.create();
		b.index(1);
		b.setGraphSize(g,nodeSize,edgeSize,dependencySize);

		// now parse the feature library
		String[] features = split(parts[3],EDGE_DELIM);

		// now parse out each edge
		int[] nodes = {-1,-1};
		for (int next=last; next!=-1; last=next+1) {
			next = string.indexOf(TAB,last);

			int srcDest = string.indexOf(SRC_DST_DELIM,last);
			int edgeDelim = string.indexOf(EDGE_DELIM,srcDest);
			String[] edgeFeatures = split(next<0?string.substring(edgeDelim+1):string.substring(edgeDelim+1,next),EDGE_FEATURE_DELIM);
			nodes[0] = Integer.parseInt(string.substring(last,srcDest));
			nodes[1] = Integer.parseInt(string.substring(srcDest+2,edgeDelim));
			// As it turns out, a trove map is slower here
			//TObjectDoubleHashMap<String> fd = new TObjectDoubleHashMap<String>();
			HashMap<String,Double> fd = new HashMap<String,Double>();
			for (String f : edgeFeatures) {
				int wtDelim = f.indexOf(FEATURE_WEIGHT_DELIM);
				int featureId = Integer.parseInt(wtDelim<0?f:f.substring(0,wtDelim));
				double featureWt = wtDelim<0?1.0:Double.parseDouble(f.substring(wtDelim+1));
				fd.put(features[featureId-1],featureWt);
			}
			b.addOutlink(g,nodes[0],new RWOutlink(fd,nodes[1]));
		}
		b.freeze(g);
		return g;
	}

	public static String[] split(String string, char delim) {
		if (string.length() == 0) return new String[0];
		int nitems=1;
		for (int i=0; i<string.length(); i++) if (string.charAt(i) == delim) nitems++;
		return split(string,delim,nitems);
	}
	public static String[] split(String string, char delim, int nitems) {
		String[] items = new String[nitems];
		int last=0;
		for (int next=last,i=0; i<items.length && next!=-1; last=next+1,i++) {
			next=string.indexOf(delim,last);
			items[i]=next<0?string.substring(last):string.substring(last,next);
		}
		return items;		
	}
}