package com.agcurations.aggallerymanager;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GlobalClassTest {

    @Test
    public void cleanHTMLCodedCharacters() {

        String sTestData = "content://com.android.externalstorage.documents/tree/3031-3461%3AArchive/document/3031-3461%3AArchive%2FAGGalleryManager%2FComics%2F319549%2Frtrktzhppzdkfgkq4gb5arb56i";
        String sExpectedResult = "content://com.android.externalstorage.documents/tree/3031-3461:Archive/document/3031-3461:Archive/AGGalleryManager/Comics/319549/rtrktzhppzdkfgkq4gb5arb56i";

        String sResult = "";
        sResult = GlobalClass.cleanHTMLCodedCharacters(sTestData);

        assertEquals(sResult, sExpectedResult);

    }

    @Test
    public void Test_GetRelativePathFromUriString() throws UnsupportedEncodingException {
        String sDataFolder = "content://com.android.externalstorage.documents/tree/3538-3631%3AArchive%2FAGGalleryManager/document/3538-3631%3AArchive%2FAGGalleryManager";
        String sPath = "content://com.android.externalstorage.documents/tree/3538-3631%3AArchive%2FAGGalleryManager/document/3538-3631%3AArchive%2FAGGalleryManager%2FVideos%2F1%2F10089";
        String sSuccessfulResult = "1%2F10089";
        GlobalClass.gsFileSeparator = URLEncoder.encode(File.separator, StandardCharsets.UTF_8.toString());
        String sResult = GlobalClass.GetRelativePathFromUriString(sPath, sDataFolder);
        assertEquals(sResult, sSuccessfulResult);
    }

}
