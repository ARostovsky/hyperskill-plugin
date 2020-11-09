package com.jetbrains.edu.learning.checkio.options;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.settings.OauthOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

public abstract class CheckiOOptions extends OauthOptions<CheckiOAccount> {
  private final CheckiOOAuthConnector myOAuthConnector;

  protected CheckiOOptions(@NotNull CheckiOOAuthConnector oauthConnector) {
    super();
    myOAuthConnector = oauthConnector;
  }

  @Nullable
  @Override
  public CheckiOAccount getCurrentAccount() {
    return myOAuthConnector.getAccount();
  }

  @Override
  public void setCurrentAccount(@Nullable CheckiOAccount account) {
    myOAuthConnector.setAccount(account);
    MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
    if (account != null) {
      messageBus.syncPublisher(CheckiOOAuthConnector.getAuthorizationTopic()).userLoggedIn();
    }
    else {
      messageBus.syncPublisher(CheckiOOAuthConnector.getAuthorizationTopic()).userLoggedOut();
    }
  }

  @NotNull
  protected LoginListener createAuthorizeListener() {
    return new LoginListener() {
      @Override
      protected void authorize(HyperlinkEvent event) {
        myOAuthConnector.doAuthorize(() -> {
          setLastSavedAccount(getCurrentAccount());
          updateLoginLabels();
        });
      }
    };
  }
}
