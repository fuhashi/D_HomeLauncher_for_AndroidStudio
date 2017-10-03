package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

public interface SetFavoriteTaskCallback {

	void onFailedSetFavorite();
	  
	void onFinishedSetFavorite(long favoriteAssetId, long unfavoriteAssetIdAssetId);
}
