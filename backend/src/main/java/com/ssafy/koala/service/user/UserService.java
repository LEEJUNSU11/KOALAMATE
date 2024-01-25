package com.ssafy.koala.service.user;

import com.ssafy.koala.config.jwt.JwtUtil;
import com.ssafy.koala.dto.user.TokenResponse;
import com.ssafy.koala.dto.user.UserDto;
import com.ssafy.koala.model.user.UserModel;
import com.ssafy.koala.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;
	private final PasswordEncoder encoder;

	public Optional<UserDto> findUserByEmailAndPassword(String email, String password) {
		UserModel user = userRepository.findUserByEmailAndPassword(email,password).orElse(null);

		return (user != null) ? Optional.of(convertToDto(user)) : Optional.empty();
	}

	public Optional<UserDto> findUserByNicknameOrEmail(String nickname, String email) {
		UserModel user = userRepository.findUserByNicknameOrEmail(nickname, email).orElse(null);

		return (user != null) ? Optional.of(convertToDto(user)) : Optional.empty();
	}

	@Transactional
	public TokenResponse auth(UserDto dto) {
		String email = dto.getEmail();
		String password = dto.getPassword();
		Optional<UserModel> userOpt = userRepository.getUserByEmail(email);

		if(userOpt.isPresent()) { // 유저 정보가 db에 존재
			// 암호화된 password를 디코딩한 값과 입력한 패스워드 값이 다르면 null 반환
			if(!encoder.matches(password, userOpt.get().getPassword())) {
				throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
			}
		}
		else {
			throw new UsernameNotFoundException("이메일이 존재하지 않습니다.");
		}

		UserDto result = new UserDto();
		BeanUtils.copyProperties(userOpt.get(), result);
		String accessToken = jwtUtil.createAccessToken(result);
		String refreshToken = jwtUtil.createRefreshToken(result);
		// refreshToken은 redis 저장 필요

		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken(accessToken);
		tokenResponse.setRefreshToken(refreshToken);

		return tokenResponse;
	}

	public void save(UserDto newUser) {
		newUser.setPassword(encoder.encode(newUser.getPassword())); // 비밀번호 암호화
		UserModel user = convertToModel(newUser);
		userRepository.save(user);
	}

	private UserDto convertToDto(UserModel user) {
		UserDto userDto = new UserDto();
		BeanUtils.copyProperties(user, userDto);
		return userDto;
	}

	private UserModel convertToModel(UserDto user) {
		UserModel userModel = new UserModel();
		BeanUtils.copyProperties(user, userModel);
		return userModel;
	}
}
