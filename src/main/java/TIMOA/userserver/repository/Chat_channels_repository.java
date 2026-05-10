package TIMOA.userserver.repository;

import TIMOA.userserver.entity.Chat_channels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface Chat_channels_repository extends JpaRepository<Chat_channels, Long> {
}

