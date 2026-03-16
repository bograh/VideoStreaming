package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(),
                Set.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}