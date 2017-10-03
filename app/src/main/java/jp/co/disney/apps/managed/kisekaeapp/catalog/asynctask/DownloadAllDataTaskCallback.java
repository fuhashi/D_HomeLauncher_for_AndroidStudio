package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;

import com.badlogic.gdx.utils.Array;


public interface DownloadAllDataTaskCallback {

	  void onFailedAllDataDownload();
	  
	  void onFinishedAllDataDownload(Array<ContentsDataDto> ctoArray);
}
