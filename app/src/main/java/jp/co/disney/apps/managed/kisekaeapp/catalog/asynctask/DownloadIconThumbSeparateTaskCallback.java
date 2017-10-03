package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;


public interface DownloadIconThumbSeparateTaskCallback {

	//保存時のエラー
	void onFailedDownloadIconThumbsSeparate(long assetId);
		 
	//ネットワーク関連でのエラー
	void onFailedDownloadIconThumbsSeparateNetwork(long asserId);

	void onFinishedDownloadIconThumbsSeparate(long assetId);
	  
}
