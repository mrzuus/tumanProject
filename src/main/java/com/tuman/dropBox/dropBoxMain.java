package com.tuman.dropBox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class dropBoxMain {
   public static final String ACCESS_KEY ="a0uriqed2s22wpm";
   public static final String SECRET_KEY ="axq1qbngvxz88xe";
    private static final String ACCESS_TOKEN = "sl.BbOLSxI6rn_eGWYDgcvk4djL63sBIn8E4vzE5ikVGUCPr2_7r0KVZ8DpxcKMlXJURBIUBUmIL0P2v_SMsVeptDTxmq2csBMfQUKZL9MG1KRLWJ0dg1EjeVHpEN4xeNgz6hG55Vp3ZD8";
public String getAPI(){
    return ACCESS_TOKEN;
}

    public static void main(String args[]) throws DbxException, IOException {
        // Create Dropbox client
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/TUMAN").build();
        DbxClientV2  client = new DbxClientV2(config, ACCESS_TOKEN);

        // Get current account info
        FullAccount account = client.users().getCurrentAccount();
        System.out.println(account.getName().getDisplayName());
        ListFolderResult result = client.files().listFolder("");
        // Get files and folder metadata from Dropbox root directory

        while (true) {
            for (Metadata metadata : result.getEntries()) {
                System.out.println(metadata.getPathLower());
            }

            if (!result.getHasMore()) {
                break;
            }

            result = client.files().listFolderContinue(result.getCursor());
        }

        }


}
