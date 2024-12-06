package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransactionDetails;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidAccountIdException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.EmailNotificationService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;
  @Mock
  private NotificationService notificationService;

  @Autowired
  private AccountsRepository accountsRepository;

  @BeforeEach
  void prepareMock(){
    MockitoAnnotations.openMocks(this);
    accountsService.setNotificationService(notificationService);
    accountsService.getAccountsRepository().clearAccounts();
  }
  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void transferFunds() {
    Account account = new Account("1");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    Account account2 = new Account("2");
    account2.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account2);

    Mockito.doNothing().when(notificationService).notifyAboutTransfer(any(),any());
    this.accountsService.transferFunds(new TransactionDetails("1","2",BigDecimal.valueOf(1000)));
    assertThat(accountsService.getAccount("1").getBalance()).isEqualTo(BigDecimal.ZERO);
    assertThat(accountsService.getAccount("2").getBalance()).isEqualTo(BigDecimal.valueOf(2000));
  }
  @Test
  void transferFundsToInvalidAccount() {
    Account account = new Account("3");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    Account account2 = new Account("4");
    account2.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account2);
    Mockito.doNothing().when(notificationService).notifyAboutTransfer(any(),any());
      try {
        this.accountsService.transferFunds(new TransactionDetails("1", "3", BigDecimal.valueOf(1000)));
      }catch(InvalidAccountIdException ex ){
        assertThat(ex.getMessage()).isEqualTo("Invalid account details");
      }
  }

  @Test
  void transferFundsWithInsufficientBalance() {
    Account account = new Account("1");
    account.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(account);

    Account account2 = new Account("2");
    account2.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account2);
    Mockito.doNothing().when(notificationService).notifyAboutTransfer(any(),any());
    try {
      this.accountsService.transferFunds(new TransactionDetails("1", "2", BigDecimal.valueOf(1000)));
    }catch(InsufficientBalanceException ex ){
      assertThat(ex.getMessage()).isEqualTo("Insufficient balance in account");
    }
  }

  @Test
  void transferFundsToSameAccount() {
    Account account = new Account("3");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    Mockito.doNothing().when(notificationService).notifyAboutTransfer(any(),any());
    try {
      this.accountsService.transferFunds(new TransactionDetails("3", "3", BigDecimal.valueOf(1000)));
    }catch(InvalidAccountIdException ex ){
      assertThat(ex.getMessage()).isEqualTo("Transfer to same account not valid");
    }
  }
}
