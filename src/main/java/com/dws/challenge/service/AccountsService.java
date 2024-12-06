package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransactionDetails;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidAccountIdException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;
    @Getter
    @Setter
    private NotificationService notificationService;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }


    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }


    public void transferFunds(TransactionDetails transactionDetails) {
        Account toAccount = getAccount(transactionDetails.getAccountToId());
        Account fromAccount = getAccount(transactionDetails.getAccountFromId());
        validateAccounts(toAccount, fromAccount);
        synchronized (this) {
            if (fromAccount.getBalance().compareTo(transactionDetails.getAmount()) >= 0) {
                accountsRepository.withdrawAmount(transactionDetails.getAccountFromId(), transactionDetails.getAmount());
                accountsRepository.depositeAmount(transactionDetails.getAccountToId(), transactionDetails.getAmount());
                notificationService.notifyAboutTransfer(fromAccount, "Amount of Rs." + transactionDetails.getAmount() + " transffered to account Id :" + transactionDetails.getAccountToId());
                notificationService.notifyAboutTransfer(toAccount, "Amount of Rs." + transactionDetails.getAmount() + " transffered from account Id :" + transactionDetails.getAccountFromId());
            } else {
                log.error("Insufficient balance in account");
                throw new InsufficientBalanceException("Insufficient balance in account");
            }
        }

    }

    private void validateAccounts(Account toAccount, Account fromAccount) {
        if ((toAccount == null) || (fromAccount == null)) {
            log.error("Invalid account details");
            throw new InvalidAccountIdException("Invalid account details");
        } else if (toAccount.getAccountId().equals(fromAccount.getAccountId())) {
            log.error("Transfer to same account");
            throw new InvalidAccountIdException("Transfer to same account not valid");
        }
    }


}
