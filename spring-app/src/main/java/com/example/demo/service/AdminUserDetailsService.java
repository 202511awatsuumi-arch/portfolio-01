package com.example.demo.service;

import com.example.demo.mapper.UserMapper;
import com.example.demo.model.UserAccount;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public AdminUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        return new User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                true,
                true,
                true,
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole()));
    }
}
