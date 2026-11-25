package com.thunai.backend;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
public class Security {
  @Component public static class Jwt {
    private final SecretKey key;
    public Jwt(@Value("${thunai.jwt.secret}") String secret){
      if(secret==null || secret.length()<32) throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
      key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String issue(Long userId){
      Date now=new Date();
      return Jwts.builder().subject(String.valueOf(userId)).issuedAt(now).expiration(new Date(now.getTime()+24L*3600*1000)).signWith(key).compact();
    }
    public Long parse(String token){ return Long.valueOf(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject()); }
  }
  @Component public static class JwtFilter extends OncePerRequestFilter {
    private final Jwt jwt; public JwtFilter(Jwt j){ this.jwt=j; }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, jakarta.servlet.ServletException {
      String h=req.getHeader("Authorization");
      if(h!=null && h.startsWith("Bearer ")){
        try { Long id=jwt.parse(h.substring(7));
          SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id,null,List.of()));
        } catch(Exception e){ SecurityContextHolder.clearContext(); }
      }
      chain.doFilter(req,res);
    }
  }
  @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
  @Bean SecurityFilterChain chain(HttpSecurity http, JwtFilter f) throws Exception {
    http.csrf(c->c.disable())
      .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a->a.requestMatchers("/api/auth/**").permitAll().anyRequest().authenticated())
      .exceptionHandling(e->e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
      .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
