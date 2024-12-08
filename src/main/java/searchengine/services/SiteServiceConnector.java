package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.entity.SiteDTO;

@Service
public class SiteServiceConnector implements ServiceMediator<SiteDTO> {
    @Override
    public SiteDTO saveEntity(SiteDTO siteDTO) {
        return siteDTO;
    }
}