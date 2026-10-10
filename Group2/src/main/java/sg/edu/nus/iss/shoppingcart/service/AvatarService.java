package sg.edu.nus.iss.shoppingcart.service;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.nus.iss.shoppingcart.entity.AccountAvatar;
import sg.edu.nus.iss.shoppingcart.repository.AccountAvatarRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;

@Service
public class AvatarService {
    private final AccountAvatarRepository avatars;
    private final UserRepository users;
    private final MessageSource messages;

    public AvatarService(AccountAvatarRepository avatars, UserRepository users, MessageSource messages) {
        this.avatars = avatars; this.users = users; this.messages = messages;
    }
    @Transactional(readOnly = true)
    public boolean exists(Long userId) { return avatars.existsById(userId); }
    @Transactional(readOnly = true)
    public Optional<byte[]> read(Long userId) { return avatars.findById(userId).map(AccountAvatar::getImage); }

    @Transactional
    public void save(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 1048576) throw invalid("avatar.uploadInvalid");
        byte[] normalized = normalize(file);
        AccountAvatar avatar = avatars.findById(userId).orElseGet(() -> new AccountAvatar(users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))));
        avatar.setImage(normalized);
        avatars.save(avatar);
    }

    private byte[] normalize(MultipartFile file) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("avatar.uploadInvalid");
            var reader = readers.next();
            try {
                if (!reader.getFormatName().equalsIgnoreCase("JPEG") && !reader.getFormatName().equalsIgnoreCase("PNG"))
                    throw invalid("avatar.uploadInvalid");
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                // Inspect dimensions before decoding, and accept only the square crop.
                if (width != height || width < 64 || width > 2048) throw invalid("avatar.cropRequired");
                BufferedImage source = reader.read(0);
                BufferedImage output = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
                var graphics = output.createGraphics();
                try {
                    graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 512, 512);
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(source, 0, 0, 512, 512, null);
                } finally { graphics.dispose(); }
                var bytes = new ByteArrayOutputStream();
                ImageIO.write(output, "jpeg", bytes);
                return bytes.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException ex) { throw invalid("avatar.uploadInvalid"); }
    }
    private ResponseStatusException invalid(String key) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, messages.getMessage(key, null, LocaleContextHolder.getLocale()));
    }
}
