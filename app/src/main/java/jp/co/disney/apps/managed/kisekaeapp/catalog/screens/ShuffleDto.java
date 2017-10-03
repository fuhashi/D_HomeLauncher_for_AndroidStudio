package jp.co.disney.apps.managed.kisekaeapp.catalog.screens;




public class ShuffleDto{

	public int		num;
	public float		dist;
	public int		col;

	public ShuffleDto( int num,float dist){
		this.num = num;
		this.dist = dist;
		this.col = 0;
	}

	public ShuffleDto( int num,float dist,int col){
		this.num = num;
		this.dist = dist;
		this.col = col;
	}
}
