package com.github.javiersantos.appupdater;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.github.javiersantos.appupdater.objects.GitHub;
import com.github.javiersantos.appupdater.objects.Update;

import java.io.IOException;

public class AppUpdateChecker {
    private Context context;
    private LibraryPreferences libraryPreferences;
    private UpdateFrom updateFrom;
    private GitHub gitHub;
    private String xmlUrl;
    private Integer showEvery;

    public AppUpdateChecker(Context context) {
        this.context = context;
        this.libraryPreferences = new LibraryPreferences(context);
        this.updateFrom = UpdateFrom.GOOGLE_PLAY;
        this.showEvery = 1;
    }

    public AppUpdateChecker setUpdateFrom(UpdateFrom updateFrom) {
        this.updateFrom = updateFrom;
        return this;
    }

    public AppUpdateChecker setUpdateXML(String xmlUrl) {
        this.xmlUrl = xmlUrl;
        return this;
    }

    public AppUpdateChecker setGitHubUserAndRepo(@NonNull String user, @NonNull String repo) {
        this.gitHub = new GitHub(user, repo);
        return this;
    }

    public Update get() throws IOException {
        valid();

        Update update = getUpdate();

        if (update == null) return null;

        if (UtilsLibrary.isUpdateAvailable(UtilsLibrary.getAppInstalledVersion(context), update.getLatestVersion())) {
            Integer successfulChecks = libraryPreferences.getSuccessfulChecks();
            if (!UtilsLibrary.isAbleToShow(successfulChecks, showEvery)) {
                update = null;
            }
            libraryPreferences.setSuccessfulChecks(successfulChecks + 1);
            return update;
        }

        return null;
    }

    private void valid() throws IOException {
        if (UtilsLibrary.isNetworkAvailable(context)) {
            if (updateFrom == UpdateFrom.GITHUB && !GitHub.isGitHubValid(gitHub)) {
                throw new IllegalArgumentException("GitHub user or repo is empty!");
            } else if (updateFrom == UpdateFrom.XML && (xmlUrl == null || !UtilsLibrary.isStringAnUrl(xmlUrl))) {
                throw new IllegalArgumentException("XML file is not valid!");
            }
        } else {
            throw new IOException("Network isn't available.");
        }
    }

    private Update getUpdate() {
        Update update;
        if (updateFrom == UpdateFrom.XML) {
            update = UtilsLibrary.getLatestAppVersionXml(xmlUrl);
            if (update == null) {
                // XML Error !!
                return null;
            }
        } else {
            update = UtilsLibrary.getLatestAppVersionHttp(context, updateFrom, gitHub);
        }

        if (UtilsLibrary.isStringAVersion(update.getLatestVersion())) {
            return update;
        } else {
            Log.e("AppUpdateChecker", "UpdateFrom.GOOGLE_PLAY isn't valid: update varies by device.");
            return null;
        }
    }
}
