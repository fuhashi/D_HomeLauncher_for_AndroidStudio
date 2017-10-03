package jp.co.disney.apps.managed.kisekaeapp.system.file;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

public class LocalCacheFileHandleResolver implements FileHandleResolver {
	@Override
	public FileHandle resolve (String fileName) {
		return Gdx.files.cache(fileName);
	}
}
