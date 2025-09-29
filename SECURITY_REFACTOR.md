# Spring Security 6 Migration Notes

This module now targets Spring Security 6 via Spring Boot 3.3.4. The upgrade modernizes authentication, authorization, and validation flows that previously depended on Spring Security 5 and the legacy `javax.*` APIs.

## Configuration Differences (Spring 5 vs 6)
- **Removal of `WebSecurityConfigurerAdapter`:** Spring Security 5 relied on subclassing `WebSecurityConfigurerAdapter` and overriding `configure(HttpSecurity)`/`configure(AuthenticationManagerBuilder)`. In Spring Security 6 the adapter is deprecated and removed. We replaced it with explicit beans:
  - `SecurityFilterChain securityFilterChain(HttpSecurity)` defines the HTTP DSL.
  - `AuthenticationManager authenticationManager(...)` wires the application `UserDetailsService` through a `DaoAuthenticationProvider`.
  - `PasswordEncoder passwordEncoder()` supplies a shared encoder bean.
- **Request Authorization DSL:** Previous `authorizeRequests().antMatchers(...).access(...)` syntax is gone. The new lambda-based DSL uses `authorizeHttpRequests(auth -> auth.requestMatchers(...).hasRole("USER")...)`.
- **H2 Console & CSRF:** `csrf().ignoringAntMatchers(...)` became `csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))`, reflecting the new `RequestMatcher` utilities shipped with Boot 3/Security 6.
- **Frame Options:** Still required for H2 but now configured via the lambda DSL (`headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))`).

## Password Handling
- Spring 5 code returned a `StandardPasswordEncoder` with a hard-coded secret. Spring Security 6 encourages delegating encoders or BCrypt. We switched to `BCryptPasswordEncoder`, removing deprecated SHA-based encoding and aligning with current recommendations.

## UserDetailsService & Repository Changes
- Spring 5 repository returned `User` directly. In Spring 6 we migrate to `Optional<User>` so the bridge can respond with `UsernameNotFoundException` without null checks.
- `UserRepositoryUserDetailsService` now unwraps the optional and throws via lambda, matching the new method reference style encouraged in Security 6.

## Controller & Form Validation
- Registration moved from `javax.validation` to `jakarta.validation` annotations. `RegistrationForm` now enforces `@NotBlank` / `@Pattern` constraints, and `RegistrationController` fails fast when `Errors` contains binding issues before hitting the repository.
- Importing `jakarta.validation.Valid` ensures form validation works with Boot 3’s default `MethodValidationPostProcessor`.

## Test Updates for Security
- Web tests no longer use `@RunWith(SpringRunner.class)`; they run on JUnit Jupiter. `@WithMockUser` remains, but Spring Security 6 requires `spring-security-test` 6.x which is pulled in via Boot 3.
- `@WebMvcTest` slices now run without the default security filter chain only when explicitly disabled (`@AutoConfigureMockMvc(addFilters = false)`), matching the tighter defaults Spring Security 6 applies.
- Browser tests still authenticate via forms; they are marked `@Disabled` until a reliable WebDriver replaces HtmlUnit under Selenium 4.

## Additional Observations
- Default security now exposes a generated user if no explicit authentication manager exists. Because we provide a custom `AuthenticationManager`, the generated user warning is suppressed in production contexts.
- Spring Security 6 expects Java 17 bytecode. Upgrading both Maven Wrapper and the JDK is mandatory to avoid `Unsupported class file major version 61` errors when loading the new security artifacts.

## Side-by-Side Comparison
| Aspect | Spring 5 (Before) | Spring 6 (After) |
| --- | --- | --- |
| HTTP configuration | `WebSecurityConfigurerAdapter` with `configure(HttpSecurity)` and chained `authorizeRequests().antMatchers()` calls | `@Bean SecurityFilterChain` with `authorizeHttpRequests()` lambda DSL and matcher ordering |
| Authentication manager | Overrode `configure(AuthenticationManagerBuilder)` to register `userDetailsService` and encoder | Declared `AuthenticationManager` bean built from `DaoAuthenticationProvider`, injected automatically |
| Password encoding | `StandardPasswordEncoder` seeded with static secret (deprecated) | `BCryptPasswordEncoder` bean shared across services |
| CSRF / H2 console | `csrf().ignoringAntMatchers("/h2-console/**")` | `csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))` using Boot 3 helpers |
| Frame options | `headers().frameOptions().sameOrigin()` nested inside adapter | `headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))` within filter-chain bean |
| Repository contract | `UserRepository` returned nullable `User` | `UserRepository` returns `Optional<User>`; service unpacks or throws `UsernameNotFoundException` |
| Registration validation | `javax.validation` imports; minimal field constraints | `jakarta.validation` with `@NotBlank`, `@Pattern`, and controller-level error checks |
| Web tests | JUnit 4 `@RunWith(SpringRunner.class)`; security filters disabled globally | JUnit 5, selective `@AutoConfigureMockMvc(addFilters = false)` and `@WithMockUser` per test |
| Browser tests | HtmlUnit driver assumed working under Selenium 3 | HtmlUnit driver hosted under Selenium 4 but marked `@Disabled` pending WebDriver replacement |

## Follow-up
1. Replace the disabled HtmlUnit Selenium tests with a supported driver and re-enable the scenarios to validate login/logout flows on Spring Security 6.
2. Consider delegating password encoding (`PasswordEncoderFactories.createDelegatingPasswordEncoder()`) if multiple encoders must coexist during user migration.
3. Review actuator security settings; Boot 3 confines endpoint exposure by default, so explicit configuration may be needed if operators access `/actuator` through secured channels.
