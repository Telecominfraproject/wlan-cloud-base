package com.whizcontrol.core.server.security;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;
import org.springframework.security.web.authentication.www.NonceExpiredException;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.whizcontrol.core.server.container.ConnectorProperties;
import com.whizcontrol.core.server.security.auth0.Auth0AuthenticationEntryPoint;
import com.whizcontrol.core.server.security.auth0.Auth0AuthenticationFilter;
import com.whizcontrol.core.server.security.auth0.Auth0AuthenticationProvider;
import com.whizcontrol.server.exceptions.ConfigurationException;

/**
 * @author dtoptygin
 *
 */
public abstract class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    protected ApplicationContext applicationContext;
    @Autowired
    private Environment environment;
    @Autowired
    private ConnectorProperties connectorProperties;

    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);
    public static final String AUTH_CACHE_NAME = "auth_details_cache";

    public static final SimpleGrantedAuthority USER_AUTHORITY = new SimpleGrantedAuthority("ROLE_USER");
    public static final SimpleGrantedAuthority MSP_AUTHORITY = new SimpleGrantedAuthority("ROLE_MSP");
    public static final SimpleGrantedAuthority SERVICE_PROVIDER_AUTHORITY = new SimpleGrantedAuthority("ROLE_SERVICE_PROVIDER");
    public static final SimpleGrantedAuthority TECH_SUPPORT_AUTHORITY = new SimpleGrantedAuthority("ROLE_TECH_SUPPORT");
    public static final SimpleGrantedAuthority CUSTOMER_EQUIPMENT_AUTHORITY = new SimpleGrantedAuthority("ROLE_CUSTOMER_EQUIPMENT");
    public static final SimpleGrantedAuthority API_AUTHORITY = new SimpleGrantedAuthority("ROLE_API");

    
    /**
     * Maximum number of auth0 provider
     */
    protected static final int MAX_AUTH0_PROVIDER = 3;
    private static final String DEFAULT_AUTH0_PROPERTY = "unknown";
    private static final String AUTH_REQUIRED_AUTHORITIES = "whizcontrol.requiredAuthorities";
    /**
     * List of path to protected for API
     */
    private static final String LIST_PROTECTED_PATH_PROP = "whizcontrol.listOfPathsToProtect";

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetailsService uds = new InMemoryUserDetailsManager(
                Arrays.asList(new UserDetails[] { new User(environment.getProperty("whizcontrol.serviceUser", "user"),
                        environment.getProperty("whizcontrol.servicePassword", "password"), true, true, true, true,
                        Arrays.asList(new SimpleGrantedAuthority[] { USER_AUTHORITY, MSP_AUTHORITY,
                                SERVICE_PROVIDER_AUTHORITY, TECH_SUPPORT_AUTHORITY, API_AUTHORITY })), }));

        return uds;
    }

    @Bean
    @Primary
    @Profile(value = { "http_digest_auth", "auth0_and_digest_auth",
            "client_certificate_and_digest_auth" })
    public DigestAuthenticationEntryPoint digestEntryPoint(Environment env) {
        DigestAuthenticationEntryPoint daep = new DigestAuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException authException) throws IOException, ServletException {

                // DTOP: this method is copied from the parent class because we
                // want to use custom Authentication Header while re-using the
                // rest of the
                // digest auth - this is so that browsers do not pop up
                // authentication dialog for REST requests
                //
                // TODO: WHEN UPDATING SPRING FRAMEWORK - make sure to sync this
                // method with its parent in the standard
                // DigestAuthenticationEntryPoint
                //

                HttpServletResponse httpResponse = (HttpServletResponse) response;

                // compute a nonce (do not use remote IP address due to proxy
                // farms)
                // format of nonce is:
                // base64(expirationTime + ":" + md5Hex(expirationTime + ":" +
                // key))
                long expiryTime = System.currentTimeMillis() + (getNonceValiditySeconds() * 1000);
                String signatureValue = md5Hex(expiryTime + ":" + getKey());
                String nonceValue = expiryTime + ":" + signatureValue;
                String nonceValueBase64 = new String(Base64.encode(nonceValue.getBytes()));

                // qop is quality of protection, as defined by RFC 2617.
                // we do not use opaque due to IE violation of RFC 2617 in not
                // representing opaque on subsequent requests in same session.
                String authenticateHeader = "Digest realm=\"" + getRealmName() + "\", " + "qop=\"auth\", nonce=\""
                        + nonceValueBase64 + "\"";

                if (authException instanceof NonceExpiredException) {
                    authenticateHeader = authenticateHeader + ", stale=\"true\"";
                }

                String wwwAuthHeaderName = "WWW-Authenticate";
                if (request.getHeader("A2W-backend") != null) {
                    // If request comes from the browser with A2W-backend header
                    // -
                    // indicate that we want to use A2W prefix for digest auth
                    // header sent from the server
                    // - to prevent browsers from popping up separate login
                    // dialog for REST calls
                    wwwAuthHeaderName = "A2W-WWW-Authenticate";
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug(wwwAuthHeaderName + " header sent to user agent: " + authenticateHeader);
                }

                httpResponse.addHeader(wwwAuthHeaderName, authenticateHeader);

                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
            }
        };
        daep.setKey(env.getProperty("whizcontrol.digestKey", "skhfgjkhgekhg3u65i7"));
        daep.setNonceValiditySeconds(
                Integer.parseInt(env.getProperty("whizcontrol.digestNonceValiditySeconds", "120")));
        daep.setRealmName(env.getProperty("whizcontrol.digestRealmName", "WhizControlRealm"));
        return daep;
    }

    /**
     * Default digest filter will not authenticate every request, it stores
     * security context between requests in a threadlocal. In most
     * server-to-server cases it is appropriate, but in browser-to-server cases
     * this may backfire, as browser can send requests with different
     * credentials over previously established connections. See how to deal with
     * this in @see CustomerAuthDigestAuthFilter
     * 
     * 
     * @param userCache
     * @param uds
     * @param daep
     * @return
     */
    @Bean
    @Profile(value = { "http_digest_auth", "auth0_and_digest_auth", "client_certificate_and_digest_auth" })
    public DigestAuthenticationFilter digestAuthenticationFilter(UserCache userCache, UserDetailsService uds,
            DigestAuthenticationEntryPoint daep) {

        DigestAuthenticationFilter daf = new DigestAuthenticationFilter();
        daf.setAuthenticationEntryPoint(daep);
        daf.setUserCache(userCache);
        daf.setUserDetailsService(uds);
        // TODO: re-visit this after integrating with centralized sign-in system
        daf.setCreateAuthenticatedToken(true);

        return daf;
    }

    @Bean
    public UserCache userCache(CacheManager cacheManager) {
        Cache cache = cacheManager.getCache(AUTH_CACHE_NAME);

        UserCache uc = null;
        try {
            uc = new SpringCacheBasedUserCache(cache);
        } catch (Exception e) {
            LOG.error("Got exception when constructing user cache", e);
            throw new ConfigurationException("Got exception when constructing user cache", e);
        }

        return uc;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        throw new IllegalStateException("WebSecurityConfig.configure method has to be overridden !!!");
    }

    protected void commonConfiguration(HttpSecurity http) {
        boolean useCsrfProtection = Boolean
                .parseBoolean(applicationContext.getEnvironment().getProperty("whizcontrol.csrf-enabled", "true"));
        if (!useCsrfProtection) {
            try {
                http.csrf().disable();
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
        }

        try {
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
        // configure content-related http headers
        try {
            HeaderWriter headerWriter = new HeaderWriter() {
                private final Set<String> cachableURL = getCachablePaths(environment);
                
                @Override
                public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
                    String requestUri = request.getRequestURI();
                    if (isCachableURL(requestUri)) {
                        return;
                    }
                    if (requestUri.length() < 5) {
                        response.addHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                        response.addHeader("Expires", "0");
                        response.addHeader("Pragma", "no-cache");
                        return;
                    }

                    int indexOfDot = requestUri.lastIndexOf('.');

                    if (indexOfDot < 0) {
                        response.addHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                        response.addHeader("Expires", "0");
                        response.addHeader("Pragma", "no-cache");
                        return;
                    }

                    boolean useVaryHeader = false;
                    boolean usePragmaHeader = true;
                    String contentExtentionStr = requestUri.substring(indexOfDot).toLowerCase();
                    switch (contentExtentionStr) {
                    case ".eot":
                        response.setContentType("application/vnd.ms-fontobject");
                        useVaryHeader = true;
                        usePragmaHeader = false;
                        break;
                    case ".otf":
                    case ".ttf":
                        response.setContentType("application/font-sfnt");
                        useVaryHeader = true;
                        usePragmaHeader = false;
                        break;
                    case ".svg":
                        response.setContentType("image/svg+xml");
                        break;
                    case ".woff":
                        response.setContentType("application/font-woff");
                        useVaryHeader = true;
                        usePragmaHeader = false;
                        break;
                    case ".woff2":
                        response.setContentType("application/font-woff2");
                        useVaryHeader = true;
                        usePragmaHeader = false;
                        break;
                    default:
                        break;
                    }

                    if (useVaryHeader) {
                        response.addHeader("Vary", "Accept-Encoding");
                    }

                    if (usePragmaHeader) {
                        response.addHeader("Pragma", "no-cache");
                        response.addHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                        response.addHeader("Expires", "0");
                    } else {
                        response.addHeader("Cache-Control", "max-age=600");
                        response.addHeader("Expires", "0");
                    }

                }

                private boolean isCachableURL(final String requestURL) {
                    for (String url: this.cachableURL) {
                        if (requestURL.startsWith(url)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
            http.headers().addHeaderWriter(headerWriter);
            http.headers().cacheControl().disable(); // we'll add those headers
                                                     // manually in the code
                                                     // above
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
        // configure cross origin verification
        try {
            Boolean xframeSameOrigin = environment.getProperty("whizcontrol.xframeSameOrigin", Boolean.class);

            if (Boolean.TRUE.equals(xframeSameOrigin)) {
                http.headers().frameOptions().sameOrigin();
            }
        } catch (Exception e) {
            throw new ConfigurationException("failed to configure xframeSameOrigin", e);
        }
    }

    /**
     * Call this method to set up form-based http authentication for use with
     * portals Make sure com.whizcontrol.core.server.ServletContainerCustomizer
     * is using connector.setAttribute("clientAuth", "false")
     * 
     * Examples of calling web service: with self-signed certificates: 1. Call
     * our service, but get login form. From there extract _csrf token. wget
     * --no-check-certificate --ca-cert=tomcat.certificate
     * --keep-session-cookies --save-cookies cookies.txt
     * https://192.168.0.124:9090/ping 2. Log in and proceed to execute previous
     * "ping" request (its value is remembered in a session) wget
     * --no-check-certificate --ca-cert=tomcat.certificate
     * --keep-session-cookies --load-cookies cookies.txt --save-cookies
     * cookies.txt --post-data "username=user&password=password&_csrf=`grep
     * _csrf login| cut -d '"' -f 6`" https://192.168.0.124:9090/login 3. if
     * needed - make additional calls to the service with stored auth cookie
     * wget --no-check-certificate --ca-cert=tomcat.certificate
     * --keep-session-cookies --load-cookies cookies.txt --save-cookies
     * cookies.txt https://192.168.0.124:9090/ping
     * 
     * @param http
     * @throws Exception
     */
    public void configureFormLoginAuth(HttpSecurity http) {

        LOG.info("configuring form login authentication");
        try {
            http.authorizeRequests().antMatchers("/ping").permitAll()
                    // can also use in here: .anyRequest().hasRole("USER");
                    .anyRequest().authenticated();

            http.formLogin();

        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        commonConfiguration(http);
    }

    /**
     * Call this method to set up basic http authentication for use with REST
     * web services Make sure
     * com.whizcontrol.core.server.ServletContainerCustomizer is using
     * connector.setAttribute("clientAuth", "false")
     * 
     * Examples of calling web service: with self-signed certificates: wget
     * --progress=dot --output-document=- --no-check-certificate
     * --ca-cert=tomcat.certificate --http-user=user --http-password=password
     * https://192.168.0.124:9090/ping
     * 
     * @param http
     * @throws Exception
     */
    public void configureBasicHttpAuth(HttpSecurity http) {

        LOG.info("configuring basic http authentication");

        try {
            http.authorizeRequests().antMatchers("/ping").permitAll()
                    // can also use in here : .anyRequest().hasRole("USER");
                    .anyRequest().authenticated();

            http.httpBasic();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        commonConfiguration(http);

    }

    /**
     * Call this method to set up digest http authentication for use with REST
     * web services Make sure
     * com.whizcontrol.core.server.ServletContainerCustomizer is using
     * connector.setAttribute("clientAuth", "false")
     * 
     * Examples of calling web service: with self-signed certificates: wget
     * --progress=dot --output-document=- --no-check-certificate
     * --ca-cert=tomcat.certificate --http-user=user --http-password=password
     * https://192.168.0.124:9090/ping
     * 
     * @param http
     * @throws Exception
     */
    public void configureDigestHttpAuth(HttpSecurity http) {

        LOG.info("configuring digest http authentication");

        try {
            // this is required for digest authentication
            http.exceptionHandling()
                    // this entry point handles cases when request is made to a
                    // protected page and
                    // user is not yet authenticated
                    .authenticationEntryPoint(applicationContext.getBean(DigestAuthenticationEntryPoint.class));

            configureProtectedPaths(http);

        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        http.addFilter(applicationContext.getBean(DigestAuthenticationFilter.class));

        commonConfiguration(http);
    }

    public static Set<String> getProtectedPaths(Environment env) {
        Set<String> ret = new HashSet<>();

        String listOfPathsToProtect = env.getProperty(LIST_PROTECTED_PATH_PROP, "");
        if (!listOfPathsToProtect.isEmpty()) {
            for (String path : listOfPathsToProtect.split(",")) {
                if (!path.trim().isEmpty()) {
                    ret.add(path.trim());
                }
            }

        }

        return ret;
    }
    
    /**
     * List of path will not secure protect with cache control
     * 
     * @param env
     * @return
     */
    public static Set<String> getCachablePaths(Environment env) {
        Set<String> ret = new HashSet<>();

        String listOfPathsCachable = env.getProperty("whizcontrol.listOfPathsCachable", "");
        if (!listOfPathsCachable.isEmpty()) {
            for (String path : listOfPathsCachable.split(",")) {
                if (!path.trim().isEmpty()) {
                    ret.add(path.trim());
                }
            }
        }
        return ret;
    }

    protected void configureProtectedPaths(HttpSecurity http) throws Exception {
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry urlRegistry = http
                .authorizeRequests();
        // order of calls is important below:
        // pass through OPTIONS requests to enable CORS
        urlRegistry.regexMatchers(HttpMethod.OPTIONS, ".+").permitAll();
        urlRegistry.antMatchers("/ping").permitAll();
                
        Set<String> listOfPathsToProtect = getProtectedPaths(environment);
        if (!listOfPathsToProtect.isEmpty()) {
            String requiredAuthorities = environment.getProperty(AUTH_REQUIRED_AUTHORITIES);
            for (String path : listOfPathsToProtect) {
                LOG.debug("Protecting path {} and its children with authorities {}", path, requiredAuthorities);
                ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl url = urlRegistry
                        .antMatchers(path + "/**");
                url.authenticated();
                if (null != requiredAuthorities) {
                    url.hasAnyAuthority(requiredAuthorities);
                }
            }

            // allow the rest of the urls
            urlRegistry.anyRequest().permitAll();

        } else {
            String requiredAuthorities = environment.getProperty(AUTH_REQUIRED_AUTHORITIES);
            LOG.debug("Protecting all paths with authorities {}", requiredAuthorities);
            ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl url = urlRegistry.anyRequest();
            url.authenticated();
            // pickup required role
            if (null != requiredAuthorities) {
                url.hasAnyAuthority(requiredAuthorities);
            }
        }
    }

    /**
     * Call this method to set up X509 Certificate authentication for use with
     * REST web services Make sure
     * com.whizcontrol.core.server.ServletContainerCustomizer is using
     * connector.setAttribute("clientAuth", "true")
     * 
     * Examples of calling web service: with self-signed certificates: wget
     * --progress=dot --output-document=- --no-check-certificate
     * --ca-cert=tomcat.certificate --certificate=clientCertificate.pem
     * --private-key=clientPrivateKey.pem https://192.168.0.124:9090/ping
     * 
     * with self-signed rootCA and all the certificates signed by that rootCA
     * wget --progress=dot --output-document=- --no-check-certificate
     * --ca-cert=WhizControlTestRootCA.crt --certificate=Ap_1_client.crt
     * --private-key=Ap_1_client.key https://192.168.0.124:9090/ping
     * 
     * @param http
     * @throws Exception
     */
    public void configureX509CertificateAuth(HttpSecurity http) {

        LOG.info("configuring X509 client certificate authentication");

        try {
            http.exceptionHandling()
                    // this entry point handles cases when request is made to a
                    // protected page and
                    // user cannot be authenticated
                    .authenticationEntryPoint(new Http403ForbiddenEntryPoint() {
                        @Override
                        public void commence(HttpServletRequest request, HttpServletResponse response,
                                AuthenticationException arg2) throws IOException, ServletException {
                            LOG.debug("X509 client auth entry point");
                            super.commence(request, response, arg2);
                        }
                    });

            http.authorizeRequests().antMatchers("/ping").permitAll()
                    // .anyRequest().hasRole("USER");
                    .anyRequest().authenticated();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        http.addFilter(x509AuthenticationFilter());

        commonConfiguration(http);

    }

    /**
     * Call this method to set up X509 Certificate authentication AND Http
     * Digest authentication for use with REST web services X509 Client
     * certificate auth will be used on the primary server connector (
     * configured by server.port property ) Http Digest auth will be used on the
     * secondary server connector (configured by whizcontrol.secondaryPort
     * property)
     * 
     * Examples of calling web service: on primary connector: curl --request
     * POST --key local-ca/private/Ap_1_client.key --cert
     * local-ca/certs/Ap_1_client.crt --cacert
     * local-ca/certs/WhizControlTestRootCA.crt --insecure --header
     * "Content-Type: application/json; charset=utf-8" --data '{"test":42}'
     * https://localhost:9096/command
     * 
     * on secondary connector: curl --digest --user user:password --request POST
     * --cacert local-ca/certs/WhizControlTestRootCA.crt --insecure --header
     * "Content-Type: application/json; charset=utf-8" --data '{"test":42}'
     * https://localhost:7071/command
     * 
     * @param http
     * @throws Exception
     */
    public void configureX509CertificateAndHttpDigestAuth(HttpSecurity http) {

        final int primaryPort = connectorProperties.getExternalPort();
        final int secondaryPort = connectorProperties.getInternalPort();

        LOG.info("configuring X509 client certificate for port {} and Http Digest authentication for port {}",
                primaryPort, secondaryPort);

        try {
            http.exceptionHandling()
                    // these entry points handle cases when request is made to a
                    // protected page and
                    // user cannot be authenticated
                    .defaultAuthenticationEntryPointFor(
                            applicationContext.getBean(DigestAuthenticationEntryPoint.class), new RequestMatcher() {

                                @Override
                                public boolean matches(HttpServletRequest request) {
                                    // use port number in here to direct to
                                    // different authenticators
                                    return request.getLocalPort() == secondaryPort;
                                }
                            })// can also have in here: new
                              // AntPathRequestMatcher("/command"))
                    .defaultAuthenticationEntryPointFor(new Http403ForbiddenEntryPoint(), AnyRequestMatcher.INSTANCE);

            http.authorizeRequests().antMatchers("/ping").permitAll()
                    // can also have in here: .anyRequest().hasRole("USER");
                    .anyRequest().authenticated();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        http.addFilter(x509AuthenticationFilter());

        http.addFilter(applicationContext.getBean(DigestAuthenticationFilter.class));

        commonConfiguration(http);

    }

    @Bean
    @Profile("client_certificate_and_basic_auth")
    protected BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint baep = new BasicAuthenticationEntryPoint();
        baep.setRealmName("WhizControl");
        return baep;
    }

    @Bean
    @Profile("client_certificate_and_basic_auth")
    protected BasicAuthenticationFilter basicAuthenticationFilter() {
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProviders.add(daoAuthenticationProvider);
        AuthenticationManager authenticationManager = new ProviderManager(authenticationProviders);

        BasicAuthenticationFilter baf = new BasicAuthenticationFilter(authenticationManager);

        return baf;
    }

    /**
     * Call this method to set up X509 Certificate authentication AND Http Basic
     * authentication for use with REST web services X509 Client certificate
     * auth will be used on the primary server connector ( configured by
     * server.port property ) Http Basic auth will be used on the secondary
     * server connector (configured by whizcontrol.secondaryPort property)
     * 
     * Examples of calling web service: on primary connector: curl --request
     * POST --key local-ca/private/Ap_1_client.key --cert
     * local-ca/certs/Ap_1_client.crt --cacert
     * local-ca/certs/WhizControlTestRootCA.crt --insecure --header
     * "Content-Type: application/json; charset=utf-8" --data '{"test":42}'
     * https://localhost:9096/command
     * 
     * on secondary connector: curl --user user:password --request POST
     * --insecure --header "Content-Type: application/json; charset=utf-8"
     * --data
     * '{"_type":"CEGWBlinkRequest","colour1":"blue","colour2":"red","equipmentQRCode":"blah"}'
     * https://localhost:7071/command
     * 
     * @param http
     */
    public void configureX509CertificateAndHttpBasicAuth(HttpSecurity http) {
        final int primaryPort = connectorProperties.getExternalPort();
        final int secondaryPort = connectorProperties.getInternalPort();

        LOG.info("configuring X509 client certificate for port {} and Http Basic authentication for port {}",
                primaryPort, secondaryPort);

        try {

            http.exceptionHandling()
                    // these entry points handle cases when request is made to a
                    // protected page and
                    // user cannot be authenticated
                    .defaultAuthenticationEntryPointFor(applicationContext.getBean(BasicAuthenticationEntryPoint.class),
                            new RequestMatcher() {

                                @Override
                                public boolean matches(HttpServletRequest request) {
                                    // use port number in here to direct to
                                    // different authenticators
                                    return request.getLocalPort() == secondaryPort;
                                }
                            })// can also have in here: new
                              // AntPathRequestMatcher("/command"))
                    .defaultAuthenticationEntryPointFor(new Http403ForbiddenEntryPoint(), AnyRequestMatcher.INSTANCE);

            http.authorizeRequests().antMatchers("/ping").permitAll()
                    // can also have in here: .anyRequest().hasRole("USER");
                    .anyRequest().authenticated();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        http.addFilter(x509AuthenticationFilter());

        http.addFilter(applicationContext.getBean(BasicAuthenticationFilter.class));

        commonConfiguration(http);
    }

    @Bean
    @Profile(value = { "client_certificate_auth", "client_certificate_and_digest_auth",
            "client_certificate_and_basic_auth" })
    public X509AuthenticationFilter x509AuthenticationFilter() {
        // enable X509 certificate auth
        X509AuthenticationFilter x509AuthenticationFilter = new X509AuthenticationFilter();
        X509PrincipalExtractor principalExtractor = new SubjectDnX509PrincipalExtractor();
        x509AuthenticationFilter.setPrincipalExtractor(principalExtractor);

        AuthenticationDetailsSource<HttpServletRequest, PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails> authenticationDetailsSource = 
                new AuthenticationDetailsSource<HttpServletRequest, PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails>() {
            @Override
            public PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails buildDetails(HttpServletRequest context) {
                List<GrantedAuthority> userGas = new ArrayList<>();

                userGas.add(CUSTOMER_EQUIPMENT_AUTHORITY);

                PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails result = new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(
                        context, userGas);

                return result;
            }
        };
        x509AuthenticationFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
        PreAuthenticatedAuthenticationProvider x505AuthenticationProvider = new PreAuthenticatedAuthenticationProvider();
        AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> preAuthenticatedUserDetailsService = new PreAuthenticatedGrantedAuthoritiesUserDetailsService();
        x505AuthenticationProvider.setPreAuthenticatedUserDetailsService(preAuthenticatedUserDetailsService);
        authenticationProviders.add(x505AuthenticationProvider);
        AuthenticationManager authenticationManager = new ProviderManager(authenticationProviders);
        x509AuthenticationFilter.setAuthenticationManager(authenticationManager);

        return x509AuthenticationFilter;
    }

    // This is only used with basic and form-based authentication
    @Override
    protected void configure(AuthenticationManagerBuilder auth) {

        try {
            auth.userDetailsService(applicationContext.getBean(UserDetailsService.class));
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

    }

    static String md5Hex(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!", e);
        }

        return new String(Hex.encode(digest.digest(data.getBytes())));
    }

    protected void configureAuth0AndHttpDigestAuth(HttpSecurity http) {
        LOG.info("configuring Auth0 and Http Digest authentication");

        try {

            Auth0AuthenticationEntryPoint auth0AuthenticationEntryPoint = new Auth0AuthenticationEntryPoint();

            http.exceptionHandling()
                    // these entry points handle cases when request is made to a
                    // protected page and
                    // user cannot be authenticated
                    .defaultAuthenticationEntryPointFor(auth0AuthenticationEntryPoint, new RequestMatcher() {

                        @Override
                        public boolean matches(HttpServletRequest request) {
                            return Auth0AuthenticationFilter.getToken(request) != null;
                        }
                    }).defaultAuthenticationEntryPointFor(
                            applicationContext.getBean(DigestAuthenticationEntryPoint.class), new RequestMatcher() {

                                @Override
                                public boolean matches(HttpServletRequest request) {
                                    return Auth0AuthenticationFilter.getToken(request) == null;
                                }
                            });

            configureProtectedPaths(http);

            List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
            for (int i = 0; i < MAX_AUTH0_PROVIDER; ++i) {
                Auth0AuthenticationProvider auth0AuthenticationProvider = createAuth0AuthenticationProvider(i);
                if (null == auth0AuthenticationProvider) {
                    break;
                }
                authenticationProviders.add(auth0AuthenticationProvider);
            }
            AuthenticationManager authenticationManager = new ProviderManager(authenticationProviders);

            Auth0AuthenticationFilter auth0AuthenticationFilter = new Auth0AuthenticationFilter();
            auth0AuthenticationFilter.setAuthenticationManager(authenticationManager);
            auth0AuthenticationFilter.setEntryPoint(auth0AuthenticationEntryPoint);

            http.addFilter(applicationContext.getBean(DigestAuthenticationFilter.class));
            http.addFilterBefore(auth0AuthenticationFilter, DigestAuthenticationFilter.class);

        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        commonConfiguration(http);
    }   
    
    /**
     * Create an auth0 authentication provider based on configuration from
     * environment. Non-Primary provider will have properties name and index
     * number.
     * 
     * @param providerIndex
     * @param defaultProperties
     * @return null if clientId is not set.
     * @throws Exception
     */
    protected Auth0AuthenticationProvider createAuth0AuthenticationProvider(int providerIndex) throws Exception {
        String clientId;
        String clientSecret;
        String securedRoute;
        String accessTypeValue;
        if (0 == providerIndex) {
            clientId = environment.getProperty("whizcontrol.auth0.clientId", DEFAULT_AUTH0_PROPERTY);
            clientSecret = environment.getProperty("whizcontrol.auth0.clientSecret", DEFAULT_AUTH0_PROPERTY);
            securedRoute = environment.getProperty("whizcontrol.auth0.securedRoute", DEFAULT_AUTH0_PROPERTY);
            accessTypeValue = environment.getProperty("whizcontrol.auth0.accessType",
                    getDefaultAccessType(providerIndex));
        } else {
            clientId = environment.getProperty("whizcontrol.auth0.clientId" + providerIndex);
            clientSecret = environment.getProperty("whizcontrol.auth0.clientSecret" + providerIndex);
            securedRoute = environment.getProperty("whizcontrol.auth0.securedRoute" + providerIndex,
                    DEFAULT_AUTH0_PROPERTY);
            accessTypeValue = environment.getProperty("whizcontrol.auth0.accessType" + providerIndex,
                    getDefaultAccessType(providerIndex));
        }
        if (null == clientId) {
            return null;
        }
        
        try {
            AccessType accessType = AccessType.valueOf(accessTypeValue);
            Auth0AuthenticationProvider auth0Provider = new Auth0AuthenticationProvider(accessType);
            auth0Provider.setClientId(clientId);
            auth0Provider.setClientSecret(clientSecret);
            auth0Provider.setSecuredRoute(securedRoute);
            auth0Provider.afterPropertiesSet();
            LOG.info("Loaded configuration for auth0 provider {}", providerIndex);
            return auth0Provider;
        } catch (Exception exp) {
            LOG.error("Failed to configure auth0 provider {}: {}", providerIndex, exp.getLocalizedMessage());
            throw new ConfigurationException("Failed to configure auth0 provider", exp);
        }
    }

    /**
     * Default access type. 0 is Portal, > 0 is Mobile
     * @param index
     * @return
     */
    private static String getDefaultAccessType(int index) {
        if (0 == index) {
            return AccessType.Portal.name();
        }
        return AccessType.Mobile.name();
    }
}
