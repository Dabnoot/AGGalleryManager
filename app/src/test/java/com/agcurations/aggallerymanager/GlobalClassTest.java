package com.agcurations.aggallerymanager;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GlobalClassTest {

    @Test
    public void cleanHTMLCodedCharacters() {

        String sTestData = "content://com.android.externalstorage.documents/tree/3031-3461%3AArchive/document/3031-3461%3AArchive%2FAGGalleryManager%2FComics%2F319549%2Frtrktzhppzdkfgkq4gb5arb56i";
        String sExpectedResult = "content://com.android.externalstorage.documents/tree/3031-3461:Archive/document/3031-3461:Archive/AGGalleryManager/Comics/319549/rtrktzhppzdkfgkq4gb5arb56i";

        String sResult = "";
        sResult = GlobalClass.cleanHTMLCodedCharacters(sTestData);

        assertEquals(sResult, sExpectedResult);

    }
}