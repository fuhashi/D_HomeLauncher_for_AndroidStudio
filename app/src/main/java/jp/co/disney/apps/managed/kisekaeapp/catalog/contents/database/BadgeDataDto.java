package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database;

public class BadgeDataDto {

	public long		assetID;
	public int		contentsType;
	public int  	detailContentsType;	
	public String	addedDate;
	
	public BadgeDataDto() {
		super();
		this.assetID = 0;
		this.contentsType = 0;
		this.detailContentsType = 0;
		this.addedDate = "";
		
	}

}
