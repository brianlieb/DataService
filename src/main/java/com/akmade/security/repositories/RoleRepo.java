package com.akmade.security.repositories;

import static com.akmade.security.util.RepositoryUtility.*;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.criterion.Restrictions;

import com.akmade.security.data.Role;
import com.akmade.security.data.RoleType;
import com.akmade.security.data.User;
import com.akmade.security.data.UserRole;
import com.akmade.security.data.UserRoleId;
import com.akmade.security.util.Qry;
import com.akmade.security.util.SessionUtility.CritQuery;
import com.akmade.security.util.Transaction.Txn;
import com.akmade.messaging.security.dto.SecurityDTO;

public class RoleRepo  {
	
	
	private static CritQuery roleQuery = 
			session -> session.createCriteria(Role.class, "role");

	private static CritQuery roleTypeQuery = 
			session -> session.createCriteria(RoleType.class, "roleType");

			
	
	protected static Function<String, Qry<Role>> getRole =
		role -> 
			session -> {
				try {
					return (Role)roleQuery
									.apply(session)
									.add(Restrictions.eq("role", role))
									.uniqueResult();
				} catch(Exception e){
					throw logAndThrowError("Error getting role.", e);
				}
			};
			
	@SuppressWarnings("unchecked")
	protected static Qry<Collection<RoleType>> getRoleTypes = 
	session -> {
			try {
				return roleTypeQuery
						.apply(session)
						.list();
			} catch (Exception e) {
				throw logAndThrowError("Error getting role types. " + e.getMessage());
			} 
		};
	
	protected static Function<String, Qry<RoleType>> getRoleTypeByType = 
		type -> 
			session -> {
				try {
					return (RoleType) roleTypeQuery
							.apply(session)
							.add( Restrictions.eq("type", type))
							.uniqueResult();
				} catch(Exception e){
					throw logAndThrowError("Error getting role type.", e);
				}
			};
	
	
	protected static Function<RoleType, Txn> saveType =
			roleType -> prepareTransaction.apply(save).apply(roleType);

	protected static Function<Collection<RoleType>, Txn> saveTypes =
			roleTypes ->  prepareTransaction(save, roleTypes);
			
	protected static Function<RoleType, Txn> deleteType =
			roleType -> prepareTransaction.apply(delete).apply(roleType);

	protected static Function<Collection<RoleType>, Txn> deleteTypes =
			roleTypes ->  prepareTransaction(delete, roleTypes);
	
	protected static Function<SecurityDTO.Role, Qry<Role>> getDBRole =
			dto ->
				session ->  getRole.apply(dto.getRole()).execute(session);
				
	protected static BiFunction<User, SecurityDTO.Role, Qry<UserRole>> makeUserRole = (user,
			dto) -> session -> {
				Role role = getDBRole.apply(dto).execute(session);
				return role != null ? new UserRole(new UserRoleId(user.getUserId(), role.getRoleId()), role, user)
						: null;
			};

	protected static BiFunction<User, SecurityDTO.Role, Qry<UserRole>> getUserRole = 
		(user, dto) -> 
			session -> {
				UserRole uRole = UserRepo.getUserRole.apply(user.getUserId()).apply(dto.getRole()).execute(session);
				return uRole == null ? makeUserRole.apply(user, dto).execute(session) : null;
			};

	protected static BiFunction<User, Collection<SecurityDTO.Role>, Qry<Collection<UserRole>>> makeUserRoles = (
			user, roles) -> session -> roles.stream().map(role -> getUserRole.apply(user, role).execute(session))
					.collect(Collectors.toSet());
			
	protected static Function<Collection<RoleType>, Collection<SecurityDTO.Type>> makeNewRoleTypesDTOs =
			at -> at
					.stream()
					.map(t -> SecurityDTO.Type.newBuilder()
								.setType(t.getType())
								.setDescription(t.getDescription())
								.build())
					.collect(Collectors.toList());
			
	protected static Qry<Collection<SecurityDTO.Type>> getRoleTypeDTOS = 
			session -> makeNewRoleTypesDTOs.apply(getRoleTypes.execute(session));
				
	
	protected static Function<Role, SecurityDTO.Role> makeNewRoleDTO = 
			role -> SecurityDTO
						.Role.newBuilder()
							.setRole(role.getRole())
							.setRoleType(SecurityDTO.Role.RoleType.valueOf(role.getRoleType().getType()))
							.setDescription(role.getDescription())
							.build();
				
	protected static Function<Collection<UserRole>, Collection<SecurityDTO.Role>> makeNewRoleDTOs =
				userRoles -> userRoles
								.parallelStream()
								.map(ur -> makeNewRoleDTO.apply(ur.getRole()))
								.collect(Collectors.toList());

	protected static Function<RoleType, Function<SecurityDTO.Type, RoleType>> mutateRoleType =
			oldRoleType ->
					dto -> {
								oldRoleType.setType(dto.getType());
								oldRoleType.setDescription(dto.getDescription());
								return oldRoleType;
					};
						
	protected static Function<SecurityDTO.Type, RoleType> makeRoleType =
			dto -> new RoleType(dto.getType(), dto.getDescription(), null);
			
	protected static BiPredicate<UserRole, UserRole> isSameUserRole = (uc,
			uc2) -> uc.getId().getRoleId() == uc2.getId().getRoleId()
					&& uc.getId().getUserId() == uc2.getId().getUserId();

	protected static Function<Collection<UserRole>, Predicate<UserRole>> containsUserRole = userRoles -> ur -> userRoles
			.stream().anyMatch(our -> isSameUserRole.test(our, ur));

	protected static BiFunction<Collection<UserRole>, Collection<UserRole>, Txn> deleteUserRoles = 
			(oldUserRoles, newUserRoles) -> 
				session -> {
				oldUserRoles.stream()
							.filter(ur -> containsUserRole.apply(newUserRoles).test(ur) != true)
							.map(ur -> UserRepo.deleteUserRole.apply(ur))
							.reduce(doNothing, Txn::andThen)
							.execute(session);
			};
			
	protected static BiFunction<User, Collection<SecurityDTO.Role>, Txn> persistUserRoles = 
		(user, roles) -> 
			session -> {
				Collection<UserRole> newUserRoles = RoleRepo.makeUserRoles.apply(user, roles).execute(session);
				deleteUserRoles.apply(user.getUserRoles(), newUserRoles)
						.andThen(UserRepo.saveUserRoles.apply(newUserRoles)).execute(session);
			};
			
	protected static Function<SecurityDTO.Type, Txn> persistRoleType =
			roleTypeDTO ->
				session -> {
					RoleType roleType =	getRoleTypeByType.apply(roleTypeDTO.getType()).execute(session);
					if (roleType!=null)
						saveType.apply(mutateRoleType.apply(roleType).apply(roleTypeDTO)).execute(session);
					else
						saveType.apply(makeRoleType.apply(roleTypeDTO)).execute(session);;
				};

	protected static Function<SecurityDTO.Type, Txn> deleteRoleType =
			roleTypeDTO ->
				session -> deleteType.apply(getRoleTypeByType.apply(roleTypeDTO.getType()).execute(session)).execute(session);;


}
