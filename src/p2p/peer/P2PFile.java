package p2p.peer;

import java.util.TreeMap;

public class P2PFile {
	private String fileName;
	private TreeMap<Long, byte[]> chunks;
	private long size;
	
	public void setSize(long size) {
		this.size = size;
	}
	
	public long getSize() {
		return size;
	}

	public TreeMap<Long, byte[]> getChunks() {
		return this.chunks;
	}
	
	public String getFileName() {
		return this.fileName;
	}
		
	public synchronized boolean complete() {
		long total = 0;
		
		for (Long key : this.chunks.keySet()) {
			total += this.chunks.get(key).length;
		}
		
		return total >= this.size;
	}
		
	public P2PFile(String filename, long size) {
		this.fileName = filename;
		this.size = size;
		this.chunks = new TreeMap<Long, byte[]>();
	}
}
