package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import searchengine.dto.entity.PageDTO;

@Service
@RequiredArgsConstructor
public class PageServiceConnector implements ServiceMediator<PageDTO> {

    private final PageService pageService;

    public void savePage(Connection.Response data, String link) {
        PageDTO pageDTO = new PageDTO();

        pageDTO.setContent(data.contentType());
        pageDTO.setCode(data.statusCode());
        pageDTO.setPath("/");

        pageService.save(pageDTO);
    }

    @Override
    public PageDTO saveEntity(PageDTO pageDTO) {
        return pageDTO;
    }
}