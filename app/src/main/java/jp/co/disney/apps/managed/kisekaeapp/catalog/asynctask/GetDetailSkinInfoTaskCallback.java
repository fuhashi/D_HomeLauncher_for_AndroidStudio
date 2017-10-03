package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ThumbInfo;

import com.badlogic.gdx.utils.Array;

public interface GetDetailSkinInfoTaskCallback {

//	  void onFailedDownloadDetailThumbs();
	  
	  void onFailedDownloadDetailThumbsNetwork();

//	  void onFinishedGetDetailInfo(Array<ContentsFileType> typeArray);
	  void onFinishedGetDetailInfo(Array<ThumbInfo> typeArray);
	  
	  
}
