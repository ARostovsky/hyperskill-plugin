package com.jetbrains.edu.learning.checkio.connectors;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import com.jetbrains.edu.learning.EduLogInListener;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer;
import com.jetbrains.edu.learning.authUtils.OAuthUtils;
import com.jetbrains.edu.learning.authUtils.TokenInfo;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo;
import com.jetbrains.edu.learning.checkio.api.CheckiOOAuthInterface;
import com.jetbrains.edu.learning.checkio.api.RetrofitUtils;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException;
import com.jetbrains.edu.learning.checkio.notifications.CheckiONotifications;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public abstract class CheckiOOAuthConnector {
  private static final Logger LOG = Logger.getInstance(CheckiOOAuthConnector.class);
  private static final CheckiOOAuthInterface CHECKIO_OAUTH_INTERFACE = RetrofitUtils.createRetrofitOAuthInterface();
  private static final Topic<EduLogInListener> myAuthorizationTopic = Topic.create("Edu.checkioUserLoggedIn", EduLogInListener.class);

  private final String myClientId;
  private final String myClientSecret;
  @NotNull private MessageBusConnection myAuthorizationBusConnection = ApplicationManager.getApplication().getMessageBus().connect();

  protected CheckiOOAuthConnector(@NotNull String clientId, @NotNull String clientSecret) {
    myClientId = clientId;
    myClientSecret = clientSecret;
  }

  public static Topic<EduLogInListener> getAuthorizationTopic() {
    return myAuthorizationTopic;
  }

  @Nullable
  public abstract CheckiOAccount getAccount();

  public abstract void setAccount(@Nullable CheckiOAccount account);

  @NotNull
  protected abstract String getOAuthServicePath();

  @NotNull
  protected abstract String getPlatformName();

  public String getAccessToken() throws CheckiOLoginRequiredException, ApiException {
    final CheckiOAccount currentAccount = requireUserLoggedIn();
    ensureTokensUpToDate();

    return currentAccount.getAccessToken();
  }

  @NotNull
  private TokenInfo getTokens(@NotNull String code, @NotNull String redirectUri) throws ApiException {
    requireClientPropertiesExist();

    return CHECKIO_OAUTH_INTERFACE.getTokens(
      OAuthUtils.GrantType.AUTHORIZATION_CODE,
      myClientSecret,
      myClientId,
      code,
      redirectUri
    ).execute();
  }

  @NotNull
  private static CheckiOUserInfo getUserInfo(@NotNull String accessToken) throws ApiException {
    return CHECKIO_OAUTH_INTERFACE.getUserInfo(accessToken).execute();
  }

  @NotNull
  private TokenInfo refreshTokens(@NotNull String refreshToken) throws ApiException {
    requireClientPropertiesExist();

    return CHECKIO_OAUTH_INTERFACE.refreshTokens(
      OAuthUtils.GrantType.REFRESH_TOKEN,
      myClientSecret,
      myClientId,
      refreshToken
    ).execute();
  }

  private void ensureTokensUpToDate() throws CheckiOLoginRequiredException, ApiException {
    final CheckiOAccount currentAccount = requireUserLoggedIn();

    if (!currentAccount.isTokenUpToDate()) {
      final String refreshToken = currentAccount.getRefreshToken();
      if (refreshToken == null) {
        LOG.error("Cannot get refresh token");
        return;
      }
      final TokenInfo newTokens = refreshTokens(refreshToken);
      currentAccount.setTokenExpiresIn(newTokens.getExpiresIn());
      currentAccount.saveTokens(newTokens);
    }
  }

  private void requireClientPropertiesExist() {
    final Pattern spacesStringPattern = Pattern.compile("\\p{javaWhitespace}*");
    if (spacesStringPattern.matcher(myClientId).matches() || spacesStringPattern.matcher(myClientSecret).matches()) {
      final String errorMessage = "Client properties are not provided";
      LOG.error(errorMessage);
      throw new IllegalStateException(errorMessage);
    }
  }

  @NotNull
  private CheckiOAccount requireUserLoggedIn() throws CheckiOLoginRequiredException {
    final CheckiOAccount currentAccount = getAccount();
    if (currentAccount == null) {
      throw new CheckiOLoginRequiredException();
    }
    return currentAccount;
  }

  public void doAuthorize(@NotNull Runnable... postLoginActions) {
    requireClientPropertiesExist();

    try {
      final String handlerUri = getOAuthHandlerUri();
      final URI oauthLink = getOauthLink(handlerUri);

      createAuthorizationListener(postLoginActions);
      BrowserUtil.browse(oauthLink);
    }
    catch (URISyntaxException | IOException e) {
      // IOException is thrown when there're no available ports, in some cases restarting can fix this
      Notifications.Bus.notify(CheckiONotifications.error(
        EduCoreBundle.message("notification.title.failed.to.authorize"),
        null,
        EduCoreBundle.message("notification.content.try.to.restart.ide.and.log.in.again")
      ));
    }
  }

  @NotNull
  private String getOAuthHandlerUri() throws IOException {
    if (EduUtils.isAndroidStudio()) {
      return getCustomServer().getHandlingUri();
    }
    else {
      final int port = BuiltInServerManager.getInstance().getPort();

      if (port < 63342 || port > 63362) {
        throw new IOException("No ports available");
      }

      return buildRedirectUri(port);
    }
  }

  @NotNull
  private URI getOauthLink(@NotNull String oauthRedirectUri) throws URISyntaxException {
    //noinspection HardCodedStringLiteral
    return new URIBuilder(CheckiONames.CHECKIO_OAUTH_URL + "/")
      .addParameter("redirect_uri", oauthRedirectUri)
      .addParameter("response_type", "code")
      .addParameter("client_id", myClientId)
      .build();
  }

  private void createAuthorizationListener(@NotNull Runnable... postLoginActions) {
    myAuthorizationBusConnection.disconnect();
    myAuthorizationBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myAuthorizationBusConnection.subscribe(myAuthorizationTopic, new EduLogInListener() {
      @Override
      public void userLoggedIn() {
        for (Runnable action : postLoginActions) {
          action.run();
        }
      }

      @Override
      public void userLoggedOut() { }
    });
  }

  @NotNull
  private String buildRedirectUri(int port) {
    return CheckiONames.CHECKIO_OAUTH_REDIRECT_HOST + ":" + port + getOAuthServicePath();
  }

  @NotNull
  private CustomAuthorizationServer getCustomServer() throws IOException {
    final CustomAuthorizationServer startedServer =
      CustomAuthorizationServer.getServerIfStarted(getPlatformName());

    if (startedServer != null) {
      return startedServer;
    }

    return createCustomServer();
  }

  private CustomAuthorizationServer createCustomServer() throws IOException {
    return CustomAuthorizationServer.create(
      getPlatformName(),
      getOAuthServicePath(),
      this::codeHandler
    );
  }

  // In case of built-in server
  @Nullable
  public String codeHandler(@NotNull String code) {
    return codeHandler(code, buildRedirectUri(BuiltInServerManager.getInstance().getPort()));
  }

  // In case of Android Studio
  @Nullable
  private synchronized String codeHandler(@NotNull String code, @NotNull String handlingPath) {
    try {
      if (getAccount() != null) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(myAuthorizationTopic).userLoggedIn();
        return "You're logged in already";
      }

      final TokenInfo tokens = getTokens(code, handlingPath);
      final CheckiOUserInfo userInfo = getUserInfo(tokens.getAccessToken());
      setAccount(new CheckiOAccount(userInfo, tokens));
      ApplicationManager.getApplication().getMessageBus().syncPublisher(myAuthorizationTopic).userLoggedIn();
      return null;
    }
    catch (NetworkException e) {
      return "Connection failed";
    }
    catch (ApiException e) {
      return "Couldn't get user info";
    }
  }
}
