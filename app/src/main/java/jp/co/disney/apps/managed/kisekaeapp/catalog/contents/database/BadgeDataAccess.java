package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database;

import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import android.content.Context;
import android.database.Cursor;

import com.badlogic.gdx.utils.Array;


public class BadgeDataAccess {

	private Context context;

	public BadgeDataAccess(Context context) {
		this.context = context;
	}

	public Array<BadgeDataDto> findMyPageDataDtoFromDB(String columnName, String value){
//		String[] columns = BaseDatabase.ART_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		
		try {
			Array<BadgeDataDto> dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
			mCursor = mItemDatabase.findTableByColumn(BaseDatabase.BADGE_SKIN_TABLE_NAME, columnName, value);//TODO
			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));

				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}

	}


	/**
	 * 全件取得
	 * @return List<AppFolderItemDto>
	 */
	public Array<BadgeDataDto> findAllSkinRecord() {
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String[] columns = BaseDatabase.SKIN_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		try {
			Array<BadgeDataDto> dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
			mCursor = mItemDatabase.queryAllTable(tableName, columns);
			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));
				
				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}
	}
	
	/**
	 * マイページに表示するスキンを全件取得（コンテンツ分けなし、MyPage内全部
	 * @return Array<MyPageDataDto
	 */
	public Array<BadgeDataDto> findAllSkin_ForMyPage() {
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String[] columns = BaseDatabase.SKIN_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		Array<BadgeDataDto> dtoList = null;
		if(dtoList != null){
			dtoList.clear();
			dtoList = null;
		}
		try {
			dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
//			mCursor = mItemDatabase.queryAllTable(tableName, columns);

			String[] col = {DataBaseParam.COL_CONTENTS_TYPE.getParam(), DataBaseParam.COL_CONTENTS_TYPE.getParam(), DataBaseParam.COL_CONTENTS_TYPE.getParam()};
			Integer[] val = {ContentsTypeValue.CONTENTS_TYPE_THEME.getValue(), ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue(), ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()};
			
			String[] col2 = {DataBaseParam.COL_FAVORITE.getParam(), DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam()};
			Integer[] val2 = {1, 1};
			
//			mCursor = mItemDatabase.findTableByColumnOr(tableName, col, val);
			mCursor = mItemDatabase.findTableByColumnOrAndOr(tableName, col, val, col2, val2);

			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));

				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			DebugLog.instance.outputLog("MyApp", "findAllAppFolderItem_valid_dtoList.size_"+dtoList.size);
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}
	}

	
	/**
	 * マイページに表示するスキンを全件取得（コンテンツ種別ごと
	 * @return Array<MyPageDataDto
	 */
	public Array<BadgeDataDto> findAllSkin_ForMyPage(int type) {
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String[] columns = BaseDatabase.SKIN_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		try {
			Array<BadgeDataDto> dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
//			mCursor = mItemDatabase.queryAllTable(tableName, columns);

			String[] col = {DataBaseParam.COL_CONTENTS_TYPE.getParam()};
			Integer[] val = {type};
			String[] col2 = {DataBaseParam.COL_FAVORITE.getParam(), DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam()};
			Integer[] val2 = {1, 1};
			mCursor = mItemDatabase.findTableByColumnOrAndOr(tableName, col, val, col2, val2);
			

			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));
				
				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			DebugLog.instance.outputLog("MyApp", "findAllAppFolderItem_valid_dtoList.size_"+dtoList.size);
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}
	}
	
	public Array<BadgeDataDto> findAllSkin_Theme(){
		DebugLog.instance.outputLog("value", "findAllSkin_Theme");
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String[] columns = BaseDatabase.SKIN_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		Array<BadgeDataDto> dtoList = null;
		if(dtoList != null){
			dtoList.clear();
			dtoList = null;
		}
		try {
			dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
//			mCursor = mItemDatabase.queryAllTable(tableName, columns);

			String[] col = {DataBaseParam.COL_CONTENTS_TYPE.getParam()};
			Integer[] val = {ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()};
			
			mCursor = mItemDatabase.findTableByColumnAnd(tableName, col, val);

			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				DebugLog.instance.outputLog("value", "findAllSkin_Theme_" + mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_THEME_TAG.getParam() )));
				
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));

				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			DebugLog.instance.outputLog("MyApp", "findAllAppFolderItem_valid_dtoList.size_"+dtoList.size);
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}

	}
	
	public Array<BadgeDataDto> findAllSkin_inThemeIntoMypage(){
		DebugLog.instance.outputLog("value", "findAllSkin_inThemeIntoMypage");
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String[] columns = BaseDatabase.SKIN_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		Array<BadgeDataDto> dtoList = null;
		if(dtoList != null){
			dtoList.clear();
			dtoList = null;
		}
		try {
			dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
//			mCursor = mItemDatabase.queryAllTable(tableName, columns);

			String[] col = {DataBaseParam.COL_CONTENTS_TYPE.getParam(),
							DataBaseParam.COL_CONTENTS_TYPE.getParam(),
							DataBaseParam.COL_CONTENTS_TYPE.getParam(),
							DataBaseParam.COL_CONTENTS_TYPE.getParam()};
			Integer[] val = {ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue(),
							ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue(),
							ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue(),
							ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()};
			
			String[] col2 = {DataBaseParam.COL_FAVORITE.getParam(), DataBaseParam.COL_EXIST.getParam()};
			Integer[] val2 = {1, 1};
			
//			mCursor = mItemDatabase.findTableByColumnOr(tableName, col, val);
			mCursor = mItemDatabase.findTableByColumnOrAndOr(tableName, col, val, col2, val2);

			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));

				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			DebugLog.instance.outputLog("MyApp", "findAllAppFolderItem_valid_dtoList.size_"+dtoList.size);
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}
		
	}

	public Array<BadgeDataDto> findAllSkin_inTheme(){
		DebugLog.instance.outputLog("value", "findAllSkin_inTheme");
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String[] columns = BaseDatabase.SKIN_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		Array<BadgeDataDto> dtoList = null;
		if(dtoList != null){
			dtoList.clear();
			dtoList = null;
		}
		try {
			dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
//			mCursor = mItemDatabase.queryAllTable(tableName, columns);

			String[] col = {DataBaseParam.COL_CONTENTS_TYPE.getParam(),
							DataBaseParam.COL_CONTENTS_TYPE.getParam(),
							DataBaseParam.COL_CONTENTS_TYPE.getParam(),
							DataBaseParam.COL_CONTENTS_TYPE.getParam()};
			Integer[] val = {ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue(),
							ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue(),
							ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue(),
							ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()};
			
			mCursor = mItemDatabase.findTableByColumnOr(tableName, col, val);

			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				DebugLog.instance.outputLog("value", "findAllSkin_inTheme_" + mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_THEME_TAG.getParam() )));
				
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));

				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			DebugLog.instance.outputLog("MyApp", "findAllAppFolderItem_valid_dtoList.size_"+dtoList.size);
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}

	}
	
	public Array<BadgeDataDto> findMyPageDataDtoFromDB(String[] columnNames, String[] values){
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String[] columns = BaseDatabase.SKIN_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		try {
			Array<BadgeDataDto> dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
//			mCursor = mItemDatabase.queryAllTable(tableName, columns);
//			String[] col = {DataBaseParam.COL_EXIST.getParam(), DataBaseParam.COL_CONTENTS_TYPE.getParam()};
//			Integer[] val = {1, type};
			mCursor = mItemDatabase.findTableByColumnAnd(tableName, columnNames, values);
			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));

				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			DebugLog.instance.outputLog("MyApp", "findAllAppFolderItem_valid_dtoList.size_"+dtoList.size);
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}
		
	}

	/**
	 * 内部ストレージに残っているスキンを全件取得
	 * @return List<AppFolderItemDto>
	 */
	public Array<BadgeDataDto> findAllSkin_isExist(int type) {
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String[] columns = BaseDatabase.SKIN_DATA_COLUMNS;
		BaseDatabase mItemDatabase = null;
		Cursor mCursor = null;
		try {
			Array<BadgeDataDto> dtoList = new Array<BadgeDataDto>();
			mItemDatabase = new BaseDatabase(context);
//			mCursor = mItemDatabase.queryAllTable(tableName, columns);
			if(type == 0){
				mCursor = mItemDatabase.findTableByColumn(tableName, DataBaseParam.COL_EXIST.getParam(), 1);
			}else{
				String[] col = {DataBaseParam.COL_EXIST.getParam(), DataBaseParam.COL_CONTENTS_TYPE.getParam()};
				Integer[] val = {1, type};
				mCursor = mItemDatabase.findTableByColumnAnd(tableName, col, val);
			}
			boolean isEof = mCursor.moveToFirst();
			while(isEof) {
				BadgeDataDto dto = new BadgeDataDto();
				dto.assetID = mCursor.getLong(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.detailContentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() ));

				dtoList.add(dto);
				isEof = mCursor.moveToNext();
			}
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			if(mCursor != null) {
				mCursor.close();
			}
			DebugLog.instance.outputLog("MyApp", "findAllAppFolderItem_valid_dtoList.size_"+dtoList.size);
			return dtoList;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::findAllAppFolderItem::Exception::" + e.toString());
			try {
				if(mItemDatabase != null){
					mItemDatabase.close();
				}
				if(mCursor != null) {
					mCursor.close();
				}
			} catch (Exception e2) {
				return null;
			}
			return null;
		}
	}

	
	/*
				dto.assetID = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_ASSET_ID.getParam() ));
				dto.addedDate = mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_ADDED_DATE.getParam() ));
				dto.contentsType = mCursor.getInt(mCursor.getColumnIndex( DataBaseParam.COL_CONTENTS_TYPE.getParam() ));
				dto.themeTag = 	mCursor.getString(mCursor.getColumnIndex( DataBaseParam.COL_THEME_TAG.getParam() ));
				dto.isExist = true;
				dto.isFavorite = true;

	 */
	/**
	 * 登録
	 * @param cto
	 * @return
	 */
	public Long insertSkinData(BadgeDataDto bto) {
		BaseDatabase mItemDatabase = null;
		bto.addedDate = "";
		
		DebugLog.instance.outputLog("value", "insert: " + bto.assetID + "/detailContentsType:" + bto.detailContentsType);
		
		try {
			mItemDatabase = new BaseDatabase(context);
			
			if(bto.addedDate == null || bto.addedDate.equals("")){
				Date d = new Date();
				String date = FileUtility.getStringFormattedDayOnly(d);
				if(date.equals(""))	return 0L;
				bto.addedDate = date;
			}

			Long count = mItemDatabase.insertBadgeSkinData(
					bto.assetID,
					bto.contentsType,
					bto.detailContentsType,
					bto.addedDate
					);
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			return count;
		} catch (Exception e) {
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			return 0L;
		}
	}

//	/**
//	 * 更新
//	 * @param dto
//	 * @return
//	 */
//	public Long updateSkinIsExist(BadgeDataDto dto, boolean isUpdate_addedDate) {
//		BaseDatabase mItemDatabase = null;
//		try {
////			DebugLog.instance.outputLog("MyApp", "ArtDataAccess_updateArtData(artdto);_dto.art_artID_"+ dto.art_artID);//
//			mItemDatabase = new BaseDatabase(context);
////			Long count = mItemDatabase.updateArtData(
//			Long count = mItemDatabase.updateSkinData_onlyisExistAndDL(
//					dto.assetID,
//					dto.isExist,
//					dto.hasDownloadHistory,
//					isUpdate_addedDate
//					);
//			if(mItemDatabase != null){
//				mItemDatabase.close();
//			}
//			return count;
//		} catch (Exception e) {
//			if(mItemDatabase != null){
//				DebugLog.instance.outputLog("MyApp", "_} catch (Exception e) {_ArtDataAccess_updateArtData(artdto);_dto.art_artID_"+ dto.assetID);//
//
//				mItemDatabase.close();
//			}
//			return 0L;
//		}
//	}

//	/**
//	 * 更新
//	 * @param dto
//	 * @return
//	 */
//	public Long updateSkinIsFavorite(ContentsDataDto dto, boolean isUpdate_addedDate) {
//		BaseDatabase mItemDatabase = null;
//		//お気に入りが外される場合は日付更新しない
//		if(!dto.isFavorite) isUpdate_addedDate = false;
//		try {
//			mItemDatabase = new BaseDatabase(context);
//			Long count = mItemDatabase.updateSkinData_onlyisFavorite(
//					dto.assetID,
//					dto.isFavorite,
//					isUpdate_addedDate
//					);
//			if(mItemDatabase != null){
//				mItemDatabase.close();
//			}
//			return count;
//		} catch (Exception e) {
//			if(mItemDatabase != null){
//				DebugLog.instance.outputLog("MyApp", "_} catch (Exception e) {_ArtDataAccess_updateArtData_onlyThumbavailable(artdto);_dto.art_artID_"+ dto.assetID);//
//				mItemDatabase.close();
//			}
//			return 0L;
//		}
//	}

//	/**
//	 * 更新
//	 * @param dto
//	 * @return
//	 */
//	public Long updateSkinIsMypage(ContentsDataDto dto, boolean isUpdate_addedDate) {
//		BaseDatabase mItemDatabase = null;
//		try {
//			mItemDatabase = new BaseDatabase(context);
//			Long count = mItemDatabase.updateSkinData_onlyisMypage(
//					dto.assetID,
//					dto.isFavorite,
//					dto.hasDownloadHistory,
//					isUpdate_addedDate
//					);
//			if(mItemDatabase != null){
//				mItemDatabase.close();
//			}
//			return count;
//		} catch (Exception e) {
//			if(mItemDatabase != null){
//				DebugLog.instance.outputLog("MyApp", "_} catch (Exception e) {_ArtDataAccess_updateArtData_updateSkinIsMypage(artdto);_dto.art_artID_"+ dto.assetID);//
//				mItemDatabase.close();
//			}
//			return 0L;
//		}
//	}

	/**
	 * 更新(コンテンツマネージャの情報から)
	 * @param dto
	 * @return
	 */
	public Long updateSkinAddedDate(BadgeDataDto dto) {
		BaseDatabase mItemDatabase = null;
		try {
//			DebugLog.instance.outputLog("MyApp", "ArtDataAccess_updateArtData(artdto);_dto.art_artID_"+ dto.art_artID);//
			mItemDatabase = new BaseDatabase(context);
			Long count = mItemDatabase.updateSkinData_onlyaddedDate(
					BaseDatabase.BADGE_SKIN_TABLE_NAME,
					dto.assetID
					);
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
			return count;
		} catch (Exception e) {
			if(mItemDatabase != null){
				DebugLog.instance.outputLog("MyApp", "_} catch (Exception e) {_ArtDataAccess_updateArtData(artdto);_dto.art_artID_"+ dto.assetID);//

				mItemDatabase.close();
			}
			return 0L;
		}
	}



//	/**
//	 * 有れば更新無ければ新規書き込み
//	 * @param dto
//	 * @return
//	 */
//	public Long upsertSkinData(ContentsDataDto dto) {
//		String tableName = BaseDatabase.MYPAGE_SKIN_TABLE_NAME;
//		String columnName = DataBaseParam.COL_ASSET_ID.getParam();
//		BaseDatabase mItemDatabase = null;
//		Cursor mCursor = null;
//		try {
//			mItemDatabase = new BaseDatabase(context);
//			mCursor = mItemDatabase.findTableByColumn(tableName, columnName,dto.assetID);
//			mCursor.moveToFirst();
//			if(mCursor.getCount() > 0) {
////				updateArtData(dto);
//				updateArtData_fromContentManager(dto);
////				DebugLog.instance.outputLog("MyApp", "updateArtData(dto);_");//
//			}else{
//				insertSkinData(dto);
////				DebugLog.instance.outputLog("MyApp", "insertArtData(dto);_");//
//			}
//			
//			if(mItemDatabase != null){
//				mItemDatabase.close();
//			}
//		}
//		finally {
//            if (mCursor != null) mCursor.close();
//        }
//
//		return 0L;
//	}
//


	/**
	 * idによる削除
	 * @param value
	 */
	public void deleteById(long assetID) {
		String tableName = BaseDatabase.BADGE_SKIN_TABLE_NAME;
		String columnName = DataBaseParam.COL_ASSET_ID.getParam();
		BaseDatabase mItemDatabase = null;
		try {
			mItemDatabase = new BaseDatabase(context);
			mItemDatabase.deleteByColumn(tableName, columnName, assetID);
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "AppFolderItemAccess::deleteById::Exception::" + e.toString());
			if(mItemDatabase != null){
				mItemDatabase.close();
			}
		}
	}


}
