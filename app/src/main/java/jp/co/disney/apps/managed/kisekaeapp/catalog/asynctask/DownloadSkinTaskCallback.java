package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;


public interface DownloadSkinTaskCallback {

	void onFailedDownloadSkin(int reason, long assetId);
	  
	void onFinishedDownloadSkin(ContentsDataDto settedCto, long delAssetIdForHistory, long delAssetIdForMypage);
}
