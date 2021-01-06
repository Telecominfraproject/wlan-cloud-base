package com.telecominfraproject.wlan.core.server.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.telecominfraproject.wlan.core.model.role.PortalUserRole;

/**
 * All authorities are defined by SimpleGrantedAuthority objects with respect to PortalUserRole.
 * Roles are assigned to PortalUsers. 
 * Authorities are set in Auth0UserDetails based on the roles found in JWT. 
 * @see WebSecurityConfig, Auth0AuthenticationProvider, Auth0UserDetails
 * 
 * @author rlee
 */
public class Authority {
    
    public static final SimpleGrantedAuthority Public = new SimpleGrantedAuthority(PortalUserRole.Public.getName());
    public static final SimpleGrantedAuthority CustomerIT = new SimpleGrantedAuthority(PortalUserRole.CustomerIT.getName());
    public static final SimpleGrantedAuthority CustomerIT_RO = new SimpleGrantedAuthority(PortalUserRole.CustomerIT_RO.getName());
    public static final SimpleGrantedAuthority ManagedServiceProvider = new SimpleGrantedAuthority(PortalUserRole.ManagedServiceProvider.getName());
    public static final SimpleGrantedAuthority ManagedServiceProvider_RO = new SimpleGrantedAuthority(PortalUserRole.ManagedServiceProvider_RO.getName());
    public static final SimpleGrantedAuthority Distributor = new SimpleGrantedAuthority(PortalUserRole.Distributor.getName());
    public static final SimpleGrantedAuthority Distributor_RO = new SimpleGrantedAuthority(PortalUserRole.Distributor_RO.getName());
    public static final SimpleGrantedAuthority TechSupport = new SimpleGrantedAuthority(PortalUserRole.TechSupport.getName());
    public static final SimpleGrantedAuthority TechSupport_RO = new SimpleGrantedAuthority(PortalUserRole.TechSupport_RO.getName());
    public static final SimpleGrantedAuthority SuperUser = new SimpleGrantedAuthority(PortalUserRole.SuperUser.getName());
    public static final SimpleGrantedAuthority SuperUser_RO = new SimpleGrantedAuthority(PortalUserRole.SuperUser_RO.getName());
    public static final SimpleGrantedAuthority Unknown = new SimpleGrantedAuthority(PortalUserRole.Unknown.getName());

}
