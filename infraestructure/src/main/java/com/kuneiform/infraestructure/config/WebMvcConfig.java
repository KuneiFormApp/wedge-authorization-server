package com.kuneiform.infraestructure.config;

import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Web MVC configuration for custom resource locations, i18n support, and Thymeleaf template
 * resolution.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final WedgeConfigProperties config;

  /**
   * Configure MessageSource for i18n support.
   *
   * @return configured MessageSource
   */
  @Bean
  @ConditionalOnMissingBean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();

    // Use the basename directly from config - ReloadableResourceBundleMessageSource
    // supports both "classpath:" and "file:" prefixes
    String basename = config.getFrontend().getI18nBasename();

    messageSource.setBasename(basename);
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setCacheSeconds(3600); // Cache for 1 hour in production, can be configured

    // Set default locale
    String defaultLocale = config.getFrontend().getDefaultLocale();
    if (StringUtils.hasText(defaultLocale)) {
      messageSource.setDefaultLocale(Locale.forLanguageTag(defaultLocale));
    }

    log.info(
        "MessageSource configured with basename: {}, default locale: {}", basename, defaultLocale);

    return messageSource;
  }

  /**
   * Configure LocaleResolver to use session-based locale with Accept-Language header fallback.
   *
   * <p>SessionLocaleResolver is used instead of AcceptHeaderLocaleResolver because:
   *
   * <ul>
   *   <li>It supports LocaleChangeInterceptor for ?lang= query parameter switching
   *   <li>It persists locale choice across requests in the session
   *   <li>AcceptHeaderLocaleResolver is read-only and cannot be changed via interceptor
   * </ul>
   *
   * @return configured LocaleResolver
   */
  @Bean
  @ConditionalOnMissingBean(name = "localeResolver")
  public LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();

    // Set default locale
    String defaultLocale = config.getFrontend().getDefaultLocale();
    resolver.setDefaultLocale(Locale.forLanguageTag(defaultLocale));

    log.info("LocaleResolver configured with default locale: {}", defaultLocale);

    return resolver;
  }

  /**
   * Configure LocaleChangeInterceptor to allow locale switching via query parameter.
   *
   * @return configured LocaleChangeInterceptor
   */
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang"); // ?lang=es or ?lang=en

    log.info("LocaleChangeInterceptor configured with parameter name: lang");

    return interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }

  @Override
  public void addViewControllers(
      org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("redirect:/login");
  }

  /** Configure custom static resource locations if provided. */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String staticPath = config.getFrontend().getStaticPath();

    if (StringUtils.hasText(staticPath)) {
      // Add external static resources location
      String resourceLocation = staticPath.endsWith("/") ? staticPath : staticPath + "/";

      registry
          .addResourceHandler("/**")
          .addResourceLocations(resourceLocation)
          .addResourceLocations("classpath:/static/"); // Fallback to classpath

      log.info("Custom static resources location configured: {}", resourceLocation);
    } else {
      // Use default classpath location
      registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");

      log.info("Using default static resources location: classpath:/static/");
    }
  }

  /** Configure Thymeleaf template resolver with custom template locations if provided. */
  @Bean
  @ConditionalOnMissingBean
  public SpringResourceTemplateResolver templateResolver() {
    SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();

    String templatesPath = config.getFrontend().getTemplatesPath();

    if (StringUtils.hasText(templatesPath)) {
      // Use external templates location
      String prefix = templatesPath.endsWith("/") ? templatesPath : templatesPath + "/";
      resolver.setPrefix(prefix);

      log.info("Custom templates location configured: {}", prefix);
    } else {
      // Use default classpath location
      resolver.setPrefix("classpath:/templates/");

      log.info("Using default templates location: classpath:/templates/");
    }

    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
    resolver.setCacheable(false); // Set to true in production

    return resolver;
  }

  /** Configure Thymeleaf template engine with custom template resolver. */
  @Bean
  @ConditionalOnMissingBean
  public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver templateResolver) {
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(templateResolver);
    engine.setEnableSpringELCompiler(true);

    return engine;
  }
}
