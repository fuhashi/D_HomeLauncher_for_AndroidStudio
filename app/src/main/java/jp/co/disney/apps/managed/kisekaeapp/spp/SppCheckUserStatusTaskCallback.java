package jp.co.disney.apps.managed.kisekaeapp.spp;

public interface SppCheckUserStatusTaskCallback {

	  void onFailedCheckUserStatus(int reason);
	  
	  void onFinishedCheckUserStatus(int businessDomain);
}
