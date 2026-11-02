package com.dev.bank.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    // no authentication yet: a later sprint replaces this with the logged-in user
    private static final String ACTOR = "system";

    private final AccountService accountService;

    public AccountController(AccountService accountService){
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponseDto createAccount(@Valid @RequestBody OpenAccountRequestDto request){
        // one new value per request, so all rows written by this request can be found together
        String correlationId = UUID.randomUUID().toString();
        Account account;
        if(request.customerId() != null){
            account = accountService.openCustomerAccount(request.customerId(), request.currencyCode(), request.glAccountCode(), ACTOR, correlationId);
        }else{
            account = accountService.openInternalAccount(request.currencyCode(), request.glAccountCode(), ACTOR, correlationId);
        }
        return AccountResponseDto.from(account);
    }

}
