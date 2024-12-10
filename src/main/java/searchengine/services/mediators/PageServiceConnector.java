package searchengine.services.mediators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.dto.entity.PageDTO;
import searchengine.services.PageService;

@Slf4j
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