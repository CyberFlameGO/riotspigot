package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import de.dytanic.log.DytanicLogger;

import java.util.Collection;
import java.util.Set;

public class YggdrasilGameProfileRepository implements GameProfileRepository
{
    private static final DytanicLogger LOGGER = DytanicLogger.getInstance();
    private static final String BASE_URL = "https://api.mojang.com/";
    private static final String SEARCH_PAGE_URL = "https://api.mojang.com/profiles/";
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;
    private final YggdrasilAuthenticationService authenticationService;
    
    public YggdrasilGameProfileRepository(final YggdrasilAuthenticationService authenticationService) {
        super();
        this.authenticationService = authenticationService;
    }
    
    @Override
    public void findProfilesByNames(final String[] names, final Agent agent, final ProfileLookupCallback callback) {
        final Set<String> criteria = Sets.newHashSet();
        for (final String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(name.toLowerCase());
            }
        }
        final int page = 0;
        for (final Object request : Iterables.partition((Iterable)criteria, 2)) {
            int failCount = 0;
            boolean failed;
            do {
                failed = false;
                try {
                    final ProfileSearchResultsResponse response = this.authenticationService.makeRequest(HttpAuthenticationService.constantURL("https://api.mojang.com/profiles/" + agent.getName().toLowerCase()), request, ProfileSearchResultsResponse.class);
                    failCount = 0;
                    YggdrasilGameProfileRepository.LOGGER.debug("Page {} returned {} results, parsing", new Object[] { page, response.getProfiles().length });
                    final Set<String> missing = (Set<String>)Sets.newHashSet((Iterable)request);
                    for (final GameProfile profile : response.getProfiles()) {
                        YggdrasilGameProfileRepository.LOGGER.debug("Successfully looked up profile {}", new Object[] { profile });
                        missing.remove(profile.getName().toLowerCase());
                        callback.onProfileLookupSucceeded(profile);
                    }
                    for (final String name2 : missing) {
                        YggdrasilGameProfileRepository.LOGGER.debug("Couldn't find profile {}", new Object[] { name2 });
                        callback.onProfileLookupFailed(new GameProfile(null, name2), new ProfileNotFoundException("Server did not find the requested profile"));
                    }
                    try {
                        Thread.sleep(100L);
                    }
                    catch (InterruptedException ex) {}
                }
                catch (AuthenticationException e) {
                    if (++failCount == 3) {
                        for (final String name3 : (Collection<String>)request) {
                            YggdrasilGameProfileRepository.LOGGER.debug("Couldn't find profile {} because of a server error", new Object[] { name3 });
                            callback.onProfileLookupFailed(new GameProfile(null, name3), e);
                        }
                    }
                    else {
                        try {
                            Thread.sleep(750L);
                        }
                        catch (InterruptedException ex2) {}
                        failed = true;
                    }
                }
            } while (failed);
        }
    }
}
