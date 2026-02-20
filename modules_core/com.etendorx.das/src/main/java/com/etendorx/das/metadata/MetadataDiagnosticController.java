package com.etendorx.das.metadata;

import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.das.metadata.models.ProjectionMetadata;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Temporary diagnostic controller for verifying DynamicMetadataService.
 * Add ?html to any endpoint for a human-friendly HTML view.
 */
@RestController
@RequestMapping("/api/metadata")
public class MetadataDiagnosticController {

    private final DynamicMetadataService metadataService;
    private final CacheManager cacheManager;
    private final DataSource dataSource;

    public MetadataDiagnosticController(DynamicMetadataService metadataService,
                                         CacheManager cacheManager,
                                         DataSource dataSource) {
        this.metadataService = metadataService;
        this.cacheManager = cacheManager;
        this.dataSource = dataSource;
    }

    @GetMapping("/projections")
    public Object listProjections(@RequestParam(name = "html", required = false) String html) {
        Set<String> names = metadataService.getAllProjectionNames();
        if (html != null) {
            return htmlResponse(renderProjectionList(names));
        }
        return names;
    }

    @GetMapping("/projections/{name}")
    public Object getProjection(@PathVariable String name,
                                @RequestParam(name = "html", required = false) String html) {
        Optional<ProjectionMetadata> projection = metadataService.getProjection(name);
        if (html != null) {
            return htmlResponse(projection.map(this::renderProjectionDetail)
                .orElse("<h1>Not Found</h1><p>Projection '" + esc(name) + "' not found.</p>"));
        }
        return projection.isPresent() ? projection.get() : Map.of("error", "Projection not found: " + name);
    }

    @PostMapping("/cache/invalidate")
    public Map<String, String> invalidateCache() {
        metadataService.invalidateCache();
        return Map.of("status", "Cache invalidated");
    }

    @GetMapping("/system")
    public Map<String, Object> systemInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // JVM
        info.put("java", Map.of(
            "version", System.getProperty("java.version"),
            "vendor", System.getProperty("java.vendor", ""),
            "vm", System.getProperty("java.vm.name", "")
        ));

        // Spring
        info.put("spring", Map.of(
            "boot", org.springframework.boot.SpringBootVersion.getVersion(),
            "profiles", System.getProperty("spring.profiles.active", "default")
        ));

        // Database
        Map<String, String> dbInfo = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            dbInfo.put("url", meta.getURL());
            dbInfo.put("product", meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            dbInfo.put("user", meta.getUserName());
        } catch (Exception e) {
            dbInfo.put("error", e.getMessage());
        }
        info.put("database", dbInfo);

        // Cache
        Map<String, Object> cacheInfo = new LinkedHashMap<>();
        org.springframework.cache.Cache springCache = cacheManager.getCache("projectionsByName");
        if (springCache != null) {
            Object nativeCache = springCache.getNativeCache();
            if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                var stats = caffeineCache.stats();
                cacheInfo.put("entries", caffeineCache.estimatedSize());
                cacheInfo.put("hitCount", stats.hitCount());
                cacheInfo.put("missCount", stats.missCount());
                cacheInfo.put("hitRate", stats.hitCount() + stats.missCount() > 0
                    ? String.format("%.1f%%", stats.hitRate() * 100) : "N/A");
                cacheInfo.put("evictionCount", stats.evictionCount());
            }
        }

        Set<String> projNames = metadataService.getAllProjectionNames();
        cacheInfo.put("projections", projNames);
        cacheInfo.put("projectionsCount", projNames.size());

        int totalEntities = 0;
        int totalFields = 0;
        for (String name : projNames) {
            Optional<ProjectionMetadata> p = metadataService.getProjection(name);
            if (p.isPresent()) {
                totalEntities += p.get().entities().size();
                totalFields += p.get().entities().stream().mapToInt(e -> e.fields().size()).sum();
            }
        }
        cacheInfo.put("totalEntities", totalEntities);
        cacheInfo.put("totalFields", totalFields);
        info.put("cache", cacheInfo);

        // Memory
        Runtime rt = Runtime.getRuntime();
        info.put("memory", Map.of(
            "max", formatBytes(rt.maxMemory()),
            "total", formatBytes(rt.totalMemory()),
            "free", formatBytes(rt.freeMemory()),
            "used", formatBytes(rt.totalMemory() - rt.freeMemory())
        ));

        return info;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        long kb = bytes / 1024;
        if (kb < 1024) return kb + " KB";
        long mb = kb / 1024;
        return mb + " MB";
    }

    @PostMapping("/cache/reload")
    public Map<String, Object> reloadCache() {
        metadataService.invalidateCache();
        metadataService.preloadCache();
        Set<String> names = metadataService.getAllProjectionNames();
        return Map.of("status", "Cache reloaded", "projections", names);
    }

    // --- HTML rendering ---

    private org.springframework.http.ResponseEntity<String> htmlResponse(String body) {
        String page = """
            <!DOCTYPE html><html><head><meta charset='utf-8'>
            <title>Metadata Explorer</title>
            <style>
            *{box-sizing:border-box}
            body{font-family:system-ui,sans-serif;margin:0;padding:2rem;background:#f0f2f5;color:#1a1a2e}
            h1{color:#16213e;margin-bottom:.3rem}
            h2{color:#0f3460;border-bottom:2px solid #e2e8f0;padding-bottom:.4rem;margin-top:2rem}
            a{color:#0d6efd;text-decoration:none} a:hover{text-decoration:underline}
            nav{margin-bottom:1.5rem;font-size:.9rem}
            .subtitle{color:#6c757d;font-size:.95rem;margin-bottom:1.5rem}

            /* Projection list */
            .proj-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:1rem;margin:1rem 0}
            .proj-card{background:#fff;border:1px solid #e2e8f0;border-radius:.75rem;padding:1.2rem;
                        transition:box-shadow .2s;cursor:pointer;text-decoration:none;color:inherit;display:block}
            .proj-card:hover{box-shadow:0 4px 12px rgba(0,0,0,.1);text-decoration:none}
            .proj-card h3{margin:0 0 .5rem;color:#16213e}
            .proj-card .stats{color:#6c757d;font-size:.85rem}

            /* Entity group card */
            .entity-group{background:#fff;border:1px solid #e2e8f0;border-radius:.75rem;
                          margin:1rem 0;overflow:hidden}
            .entity-header{background:#16213e;color:#fff;padding:.8rem 1.2rem;display:flex;
                           justify-content:space-between;align-items:center}
            .entity-header h3{margin:0;font-size:1.1rem}
            .entity-tags span{margin-left:.5rem}
            .entity-body{padding:1rem 1.2rem}

            /* Read/Write columns */
            .rw-grid{display:grid;grid-template-columns:1fr 1fr;gap:1.5rem}
            @media(max-width:900px){.rw-grid{grid-template-columns:1fr}}
            .rw-section h4{margin:0 0 .5rem;font-size:.95rem}
            .rw-section h4 .type-badge{display:inline-block;padding:.15rem .5rem;border-radius:.25rem;
                                        font-size:.75rem;font-weight:700;margin-left:.4rem}
            .read-badge{background:#d1e7dd;color:#0f5132}
            .write-badge{background:#cfe2ff;color:#084298}

            /* Field tables */
            table{border-collapse:collapse;width:100%;font-size:.85rem}
            th,td{border:1px solid #e9ecef;padding:.35rem .5rem;text-align:left}
            th{background:#f8f9fa;font-weight:600;color:#495057}
            tr:hover{background:#f1f3f5}

            /* Mapping badges */
            .badge{display:inline-block;padding:.1rem .45rem;border-radius:.2rem;font-size:.75rem;font-weight:600}
            .dm{background:#d1e7dd;color:#0f5132} .jm{background:#cff4fc;color:#055160}
            .cv{background:#fff3cd;color:#664d03} .jp{background:#e2d9f3;color:#432874}
            .em{background:#ffd6e0;color:#842029} .cm{background:#ffe5d0;color:#984c0c}

            .tag{background:#e9ecef;padding:.1rem .4rem;border-radius:.2rem;font-size:.8rem;color:#495057}
            .info-row{display:flex;gap:1rem;flex-wrap:wrap;margin-bottom:.5rem;font-size:.85rem;color:#6c757d}
            .no-fields{color:#adb5bd;font-style:italic;padding:.5rem 0}

            /* JWT Token bar */
            .token-bar{position:sticky;top:0;z-index:100;background:#16213e;padding:.6rem 1.2rem;
                       display:flex;align-items:center;gap:.8rem;margin:-2rem -2rem 1.5rem;box-shadow:0 2px 8px rgba(0,0,0,.2)}
            .token-bar label{color:#a8b2d1;font-size:.85rem;font-weight:600;white-space:nowrap}
            .token-bar input{flex:1;padding:.4rem .6rem;border:1px solid #3a4a6b;border-radius:.35rem;
                             background:#0f1b33;color:#e0e6f0;font-size:.82rem;font-family:monospace}
            .token-bar input::placeholder{color:#5a6a8a}
            .token-bar .token-status{font-size:.75rem;padding:.2rem .5rem;border-radius:.2rem}
            .token-ok{background:#d1e7dd;color:#0f5132}
            .token-none{background:#f8d7da;color:#842029}

            /* Test buttons & results */
            .test-btn{display:inline-flex;align-items:center;gap:.3rem;padding:.3rem .7rem;border:none;
                      border-radius:.3rem;background:#0d6efd;color:#fff;font-size:.8rem;font-weight:600;
                      cursor:pointer;transition:background .2s}
            .test-btn:hover{background:#0b5ed7}
            .test-btn:disabled{background:#6c757d;cursor:not-allowed}
            .test-result{margin-top:.8rem;border:1px solid #e2e8f0;border-radius:.5rem;overflow:hidden;display:none}
            .test-result-header{display:flex;justify-content:space-between;align-items:center;
                                padding:.4rem .8rem;background:#f8f9fa;border-bottom:1px solid #e2e8f0;font-size:.8rem}
            .test-result-header .status-ok{color:#0f5132;font-weight:600}
            .test-result-header .status-err{color:#842029;font-weight:600}
            .test-result-body{max-height:400px;overflow:auto;padding:.6rem .8rem;background:#1e1e1e;
                              color:#d4d4d4;font-family:monospace;font-size:.8rem;white-space:pre-wrap;word-break:break-all}
            .test-loading{color:#6c757d;font-style:italic;padding:.5rem}

            /* System Info Modal */
            .modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:200;
                           justify-content:center;align-items:center}
            .modal-overlay.active{display:flex}
            .modal{background:#fff;border-radius:.75rem;width:90%;max-width:700px;max-height:85vh;overflow:auto;
                   box-shadow:0 8px 32px rgba(0,0,0,.25)}
            .modal-header{display:flex;justify-content:space-between;align-items:center;padding:1rem 1.2rem;
                          background:#16213e;color:#fff;border-radius:.75rem .75rem 0 0}
            .modal-header h2{margin:0;font-size:1.1rem}
            .modal-close{background:none;border:none;color:#a8b2d1;font-size:1.3rem;cursor:pointer;padding:.2rem .5rem}
            .modal-close:hover{color:#fff}
            .modal-body{padding:1.2rem}
            .sys-section{margin-bottom:1.2rem}
            .sys-section h3{font-size:.95rem;color:#0f3460;margin:0 0 .5rem;border-bottom:1px solid #e2e8f0;padding-bottom:.3rem}
            .sys-grid{display:grid;grid-template-columns:140px 1fr;gap:.2rem .8rem;font-size:.85rem}
            .sys-grid .lbl{color:#6c757d;font-weight:600}
            .sys-grid .val{color:#1a1a2e;word-break:break-all}
            .sys-bar{height:8px;background:#e9ecef;border-radius:4px;overflow:hidden;margin-top:.2rem}
            .sys-bar-fill{height:100%;background:#0d6efd;border-radius:4px;transition:width .3s}
            .sys-proj-list{display:flex;flex-wrap:wrap;gap:.3rem;margin-top:.3rem}
            .sys-proj-tag{background:#e9ecef;padding:.15rem .5rem;border-radius:.2rem;font-size:.8rem;color:#495057}
            </style></head><body>""" + tokenBarHtml() + body + systemInfoModal() + tokenBarScript() + "</body></html>";
        return org.springframework.http.ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(page);
    }

    private String renderProjectionList(Set<String> names) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Metadata Explorer</h1>");
        sb.append("<p class='subtitle'>").append(names.size()).append(" projections loaded in cache</p>");
        sb.append("<div class='proj-grid'>");
        for (String name : new TreeSet<>(names)) {
            Optional<ProjectionMetadata> opt = metadataService.getProjection(name);
            int entityCount = opt.map(p -> countEntityGroups(p)).orElse(0);
            int fieldCount = opt.map(p -> p.entities().stream().mapToInt(e -> e.fields().size()).sum()).orElse(0);
            sb.append("<a class='proj-card' href='/api/metadata/projections/").append(esc(name)).append("?html'>");
            sb.append("<h3>").append(esc(name)).append("</h3>");
            sb.append("<div class='stats'>").append(entityCount).append(" entities &middot; ")
              .append(fieldCount).append(" fields</div>");
            sb.append("</a>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String renderProjectionDetail(ProjectionMetadata p) {
        // Build entity ID -> name index for resolving related entity references
        Map<String, String> entityIndex = new HashMap<>();
        for (EntityMetadata e : p.entities()) {
            String label = e.externalName() != null ? e.externalName() : e.name();
            String suffix = e.mappingType() != null ? " (" + e.mappingType() + ")" : "";
            entityIndex.put(e.id(), label + suffix);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<nav><a href='/api/metadata/projections?html'>&larr; All Projections</a>");
        sb.append(" &nbsp;|&nbsp; <a href='/api/metadata/projections/").append(esc(p.name())).append("'>JSON</a>");
        sb.append(" &nbsp;|&nbsp; <button class='test-btn' style='font-size:.85rem' onclick=\"downloadPostman()\">Export Postman Collection</button>");
        sb.append("</nav>");
        sb.append("<h1>").append(esc(p.name())).append("</h1>");
        sb.append("<div class='info-row'>");
        sb.append("<span><strong>ID:</strong> <span class='tag'>").append(esc(p.id())).append("</span></span>");
        sb.append("<span><strong>gRPC:</strong> ").append(p.grpc() ? "Yes" : "No").append("</span>");
        if (p.description() != null && !p.description().isEmpty()) {
            sb.append("<span><strong>Description:</strong> ").append(esc(p.description())).append("</span>");
        }
        sb.append("</div>");

        // Group entities by externalName
        Map<String, List<EntityMetadata>> grouped = p.entities().stream()
            .collect(Collectors.groupingBy(
                e -> e.externalName() != null ? e.externalName() : e.name(),
                LinkedHashMap::new, Collectors.toList()));

        sb.append("<h2>Entities (").append(grouped.size()).append(")</h2>");

        for (var entry : grouped.entrySet()) {
            String extName = entry.getKey();
            List<EntityMetadata> entities = entry.getValue();

            EntityMetadata readEntity = entities.stream()
                .filter(e -> "R".equals(e.mappingType())).findFirst().orElse(null);
            EntityMetadata writeEntity = entities.stream()
                .filter(e -> "W".equals(e.mappingType())).findFirst().orElse(null);
            // Fallback: if no R/W, just show all
            if (readEntity == null && writeEntity == null) {
                readEntity = entities.isEmpty() ? null : entities.get(0);
                if (entities.size() > 1) writeEntity = entities.get(1);
            }

            sb.append("<div class='entity-group'>");
            sb.append("<div class='entity-header'>");
            sb.append("<h3>").append(esc(extName)).append("</h3>");
            sb.append("<div class='entity-tags'>");
            if (readEntity != null) sb.append("<span class='badge read-badge'>READ</span>");
            if (writeEntity != null) sb.append("<span class='badge write-badge'>WRITE</span>");
            sb.append("</div></div>");

            sb.append("<div class='entity-body'>");

            // Entity info from read (or whichever exists)
            EntityMetadata ref = readEntity != null ? readEntity : writeEntity;
            if (ref != null) {
                sb.append("<div class='info-row'>");
                sb.append("<span><strong>Table:</strong> <span class='tag'>").append(esc(ref.tableId())).append("</span></span>");
                sb.append("<span><strong>REST:</strong> ").append(ref.restEndPoint() ? "Yes" : "No").append("</span>");
                sb.append("<span><strong>Identity:</strong> ").append(ref.identity() ? "Yes" : "No").append("</span>");
                sb.append("</div>");
            }

            // Read/Write side by side
            sb.append("<div class='rw-grid'>");
            if (readEntity != null) {
                sb.append("<div class='rw-section'>");
                sb.append("<h4>Read Fields <span class='type-badge read-badge'>R</span></h4>");
                renderFieldTable(sb, readEntity, entityIndex);
                sb.append("</div>");
            }
            if (writeEntity != null) {
                sb.append("<div class='rw-section'>");
                sb.append("<h4>Write Fields <span class='type-badge write-badge'>W</span></h4>");
                renderFieldTable(sb, writeEntity, entityIndex);
                sb.append("</div>");
            }
            sb.append("</div>"); // rw-grid

            // Test Read button (only for entities with REST endpoint)
            if (ref != null && ref.restEndPoint()) {
                String endpoint = "/" + p.name().toLowerCase() + "/" + extName;
                sb.append("<div style='margin-top:.8rem;display:flex;align-items:center;gap:.8rem'>");
                sb.append("<button class='test-btn' onclick=\"testEndpoint(this,'").append(esc(endpoint)).append("')\">");
                sb.append("Test GET ").append(esc(endpoint)).append("</button>");
                sb.append("<span style='font-size:.75rem;color:#6c757d'>page=0&amp;size=5</span>");
                sb.append("</div>");
                sb.append("<div class='test-result'>");
                sb.append("<div class='test-result-header'><span></span><span style='color:#6c757d'>Response</span></div>");
                sb.append("<div class='test-result-body'></div>");
                sb.append("</div>");
            }

            sb.append("</div>"); // entity-body
            sb.append("</div>"); // entity-group
        }
        // Embed entity data for Postman export
        sb.append("<script>window._projection=").append(buildPostmanData(p)).append(";</script>");

        return sb.toString();
    }

    private String buildPostmanData(ProjectionMetadata p) {
        StringBuilder js = new StringBuilder();
        js.append("{\"name\":\"").append(jsEsc(p.name())).append("\",\"entities\":[");
        Map<String, List<EntityMetadata>> grouped = p.entities().stream()
            .collect(Collectors.groupingBy(
                e -> e.externalName() != null ? e.externalName() : e.name(),
                LinkedHashMap::new, Collectors.toList()));
        boolean first = true;
        for (var entry : grouped.entrySet()) {
            String extName = entry.getKey();
            List<EntityMetadata> entities = entry.getValue();
            EntityMetadata ref = entities.stream()
                .filter(e -> "R".equals(e.mappingType())).findFirst()
                .orElse(entities.isEmpty() ? null : entities.get(0));
            if (ref == null || !ref.restEndPoint()) continue;
            boolean hasWrite = entities.stream().anyMatch(e -> "W".equals(e.mappingType()));
            EntityMetadata writeEntity = entities.stream()
                .filter(e -> "W".equals(e.mappingType())).findFirst().orElse(null);
            if (!first) js.append(",");
            first = false;
            js.append("{\"name\":\"").append(jsEsc(extName)).append("\"");
            js.append(",\"hasWrite\":").append(hasWrite);
            // Build write fields for POST body template
            if (writeEntity != null && !writeEntity.fields().isEmpty()) {
                js.append(",\"writeFields\":[");
                boolean ff = true;
                for (FieldMetadata f : writeEntity.fields()) {
                    if (!ff) js.append(",");
                    ff = false;
                    js.append("{\"name\":\"").append(jsEsc(f.name())).append("\"");
                    js.append(",\"mandatory\":").append(f.mandatory());
                    js.append(",\"mapping\":\"").append(jsEsc(f.fieldMapping().getCode())).append("\"}");
                }
                js.append("]");
            }
            js.append("}");
        }
        js.append("]}");
        return js.toString();
    }

    private static String jsEsc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void renderFieldTable(StringBuilder sb, EntityMetadata entity, Map<String, String> entityIndex) {
        if (entity.fields().isEmpty()) {
            sb.append("<p class='no-fields'>No fields configured</p>");
            return;
        }
        sb.append("<table><tr><th>Line</th><th>Field</th><th>Property</th><th>Type</th><th>Mand.</th><th>Details</th></tr>");
        for (FieldMetadata f : entity.fields()) {
            sb.append("<tr>");
            sb.append("<td>").append(f.line() != null ? f.line() : "-").append("</td>");
            sb.append("<td><strong>").append(esc(f.name())).append("</strong></td>");
            sb.append("<td>").append(esc(f.property())).append("</td>");
            sb.append("<td>").append(mappingBadge(f)).append("</td>");
            sb.append("<td>").append(f.mandatory() ? "Yes" : "-").append("</td>");
            sb.append("<td>").append(fieldDetails(f, entityIndex)).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    private int countEntityGroups(ProjectionMetadata p) {
        return (int) p.entities().stream()
            .map(e -> e.externalName() != null ? e.externalName() : e.name())
            .distinct().count();
    }

    private String mappingBadge(FieldMetadata f) {
        String code = f.fieldMapping().getCode();
        String css = switch (code) {
            case "DM" -> "dm";
            case "JM" -> "jm";
            case "CV" -> "cv";
            case "JP" -> "jp";
            case "EM" -> "em";
            case "CM" -> "cm";
            default -> "tag";
        };
        return "<span class='badge " + css + "'>" + esc(code) + "</span>";
    }

    private String fieldDetails(FieldMetadata f, Map<String, String> entityIndex) {
        List<String> parts = new ArrayList<>();
        if (f.javaMappingQualifier() != null) parts.add("qualifier: " + f.javaMappingQualifier());
        if (f.constantValue() != null) parts.add("value: " + f.constantValue());
        if (f.jsonPath() != null) parts.add("path: " + f.jsonPath());
        if (f.identifiesUnivocally()) parts.add("unique");
        if (f.relatedProjectionEntityId() != null) {
            String resolvedName = entityIndex.get(f.relatedProjectionEntityId());
            if (resolvedName != null) {
                parts.add("&rarr; " + esc(resolvedName));
            } else {
                parts.add("&rarr; " + f.relatedProjectionEntityId().substring(0, 8) + "...");
            }
        }
        if (f.createRelated()) parts.add("createRelated");
        if (parts.isEmpty()) return "-";
        // Don't double-escape since we already handle HTML in arrow entities
        return String.join(", ", parts);
    }

    private String systemInfoModal() {
        return """
            <div class='modal-overlay' id='sysinfo-modal' onclick='if(event.target===this)this.classList.remove(\"active\")'>
              <div class='modal'>
                <div class='modal-header'>
                  <h2>System Info</h2>
                  <button class='modal-close' onclick='document.getElementById(\"sysinfo-modal\").classList.remove(\"active\")'>&times;</button>
                </div>
                <div class='modal-body' id='sysinfo-body'>
                  <p style='color:#6c757d'>Loading...</p>
                </div>
              </div>
            </div>
            """;
    }

    private String tokenBarHtml() {
        return """
            <div class='token-bar'>
              <label>JWT Token:</label>
              <input type='text' id='jwt-input' placeholder='Paste your JWT token here...'/>
              <span id='token-status' class='token-status token-none'>No token</span>
              <button id='reload-btn' style='padding:.35rem .7rem;border:none;border-radius:.3rem;background:#dc3545;color:#fff;font-size:.8rem;font-weight:600;cursor:pointer;white-space:nowrap' onclick='reloadCache()'>Reload Cache</button>
              <button style='padding:.35rem .7rem;border:none;border-radius:.3rem;background:#6f42c1;color:#fff;font-size:.8rem;font-weight:600;cursor:pointer;white-space:nowrap' onclick='showSystemInfo()'>System Info</button>
            </div>
            """;
    }

    private String tokenBarScript() {
        return """
            <script>
            (function(){
              const input = document.getElementById('jwt-input');
              const status = document.getElementById('token-status');
              // Restore from localStorage
              const saved = localStorage.getItem('das_jwt_token');
              if(saved){input.value=saved; updateStatus(saved);}
              input.addEventListener('input', function(){
                const t=this.value.trim();
                localStorage.setItem('das_jwt_token',t);
                updateStatus(t);
              });
              function updateStatus(t){
                if(t){
                  status.textContent='Token set';
                  status.className='token-status token-ok';
                }else{
                  status.textContent='No token';
                  status.className='token-status token-none';
                }
              }
              // Global test function
              window.testEndpoint = async function(btn, url){
                const token = localStorage.getItem('das_jwt_token');
                if(!token){alert('Paste a JWT token first');return;}
                const resultDiv = btn.closest('.entity-group').querySelector('.test-result');
                const bodyDiv = resultDiv.querySelector('.test-result-body');
                const statusSpan = resultDiv.querySelector('.test-result-header span');
                resultDiv.style.display='block';
                bodyDiv.textContent='Loading...';
                statusSpan.textContent='...';
                statusSpan.className='';
                btn.disabled=true;
                try{
                  const sep = url.includes('?') ? '&' : '?';
                  const resp = await fetch(url+sep+'page=0&size=5', {headers:{'Authorization':'Bearer '+token}});
                  const text = await resp.text();
                  let display = text;
                  try{display=JSON.stringify(JSON.parse(text),null,2);}catch(e){}
                  bodyDiv.textContent = display;
                  if(resp.ok){
                    statusSpan.textContent=resp.status+' OK';
                    statusSpan.className='status-ok';
                  }else{
                    statusSpan.textContent=resp.status+' '+resp.statusText;
                    statusSpan.className='status-err';
                  }
                }catch(e){
                  bodyDiv.textContent='Error: '+e.message;
                  statusSpan.textContent='Failed';
                  statusSpan.className='status-err';
                }finally{btn.disabled=false;}
              };
              // System Info modal
              window.showSystemInfo = async function(){
                const modal = document.getElementById('sysinfo-modal');
                const body = document.getElementById('sysinfo-body');
                modal.classList.add('active');
                body.innerHTML = '<p style="color:#6c757d">Loading...</p>';
                try{
                  const resp = await fetch('/api/metadata/system');
                  const d = await resp.json();
                  let h = '';
                  // Java
                  h+="<div class='sys-section'><h3>Java</h3><div class='sys-grid'>";
                  h+="<span class='lbl'>Version</span><span class='val'>"+esc(d.java.version)+"</span>";
                  h+="<span class='lbl'>Vendor</span><span class='val'>"+esc(d.java.vendor)+"</span>";
                  h+="<span class='lbl'>VM</span><span class='val'>"+esc(d.java.vm)+"</span>";
                  h+="</div></div>";
                  // Spring
                  h+="<div class='sys-section'><h3>Spring Boot</h3><div class='sys-grid'>";
                  h+="<span class='lbl'>Version</span><span class='val'>"+esc(d.spring.boot)+"</span>";
                  h+="<span class='lbl'>Profiles</span><span class='val'>"+esc(d.spring.profiles)+"</span>";
                  h+="</div></div>";
                  // Database
                  h+="<div class='sys-section'><h3>Database</h3><div class='sys-grid'>";
                  h+="<span class='lbl'>URL</span><span class='val'>"+esc(d.database.url||'')+"</span>";
                  h+="<span class='lbl'>Product</span><span class='val'>"+esc(d.database.product||'')+"</span>";
                  h+="<span class='lbl'>User</span><span class='val'>"+esc(d.database.user||'')+"</span>";
                  h+="</div></div>";
                  // Cache
                  h+="<div class='sys-section'><h3>Metadata Cache</h3><div class='sys-grid'>";
                  h+="<span class='lbl'>Entries</span><span class='val'>"+d.cache.entries+"</span>";
                  h+="<span class='lbl'>Hit / Miss</span><span class='val'>"+d.cache.hitCount+" / "+d.cache.missCount+"</span>";
                  h+="<span class='lbl'>Hit Rate</span><span class='val'>"+esc(d.cache.hitRate||'N/A')+"</span>";
                  h+="<span class='lbl'>Evictions</span><span class='val'>"+d.cache.evictionCount+"</span>";
                  h+="<span class='lbl'>Projections</span><span class='val'>"+d.cache.projectionsCount+"</span>";
                  h+="<span class='lbl'>Total Entities</span><span class='val'>"+d.cache.totalEntities+"</span>";
                  h+="<span class='lbl'>Total Fields</span><span class='val'>"+d.cache.totalFields+"</span>";
                  h+="</div>";
                  if(d.cache.projections && d.cache.projections.length){
                    h+="<div class='sys-proj-list'>";
                    for(const p of d.cache.projections) h+="<span class='sys-proj-tag'>"+esc(p)+"</span>";
                    h+="</div>";
                  }
                  h+="</div>";
                  // Memory
                  h+="<div class='sys-section'><h3>Memory</h3><div class='sys-grid'>";
                  h+="<span class='lbl'>Used</span><span class='val'>"+esc(d.memory.used)+"</span>";
                  h+="<span class='lbl'>Total</span><span class='val'>"+esc(d.memory.total)+"</span>";
                  h+="<span class='lbl'>Max</span><span class='val'>"+esc(d.memory.max)+"</span>";
                  h+="</div></div>";
                  body.innerHTML = h;
                }catch(e){
                  body.innerHTML = '<p style="color:#842029">Error: '+esc(e.message)+'</p>';
                }
                function esc(s){return s==null?'':String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}
              };
              // Reload cache
              window.reloadCache = async function(){
                const btn = document.getElementById('reload-btn');
                btn.disabled=true; btn.textContent='Reloading...';
                try{
                  const resp = await fetch('/api/metadata/cache/reload',{method:'POST'});
                  const data = await resp.json();
                  btn.textContent='Reloaded!';
                  btn.style.background='#198754';
                  setTimeout(()=>window.location.reload(), 500);
                }catch(e){
                  btn.textContent='Error';
                  btn.style.background='#dc3545';
                  alert('Failed to reload: '+e.message);
                }
              };
              // Postman Collection generator
              window.downloadPostman = function(){
                const proj = window._projection;
                if(!proj){alert('No projection data');return;}
                const token = localStorage.getItem('das_jwt_token')||'';
                const baseUrl = window.location.origin;
                const prefix = proj.name.toLowerCase();
                const items = [];
                for(const entity of proj.entities){
                  const folder = {name:entity.name, item:[]};
                  const path = '/'+prefix+'/'+entity.name;
                  // GET list
                  folder.item.push({
                    name:'List '+entity.name,
                    request:{
                      method:'GET',
                      header:[{key:'Authorization',value:'Bearer {{jwt_token}}',type:'text'}],
                      url:{raw:'{{base_url}}'+path+'?page=0&size=20',
                           host:['{{base_url}}'],path:[prefix,entity.name],
                           query:[{key:'page',value:'0'},{key:'size',value:'20'}]}
                    }
                  });
                  // GET by ID
                  folder.item.push({
                    name:'Get '+entity.name+' by ID',
                    request:{
                      method:'GET',
                      header:[{key:'Authorization',value:'Bearer {{jwt_token}}',type:'text'}],
                      url:{raw:'{{base_url}}'+path+'/{{record_id}}',
                           host:['{{base_url}}'],path:[prefix,entity.name,'{{record_id}}']}
                    }
                  });
                  // POST (if write)
                  if(entity.hasWrite){
                    const body = {};
                    if(entity.writeFields){
                      for(const f of entity.writeFields){
                        if(f.mapping==='DM'||f.mapping==='EM') body[f.name] = f.mandatory ? '<required>' : '';
                      }
                    }
                    folder.item.push({
                      name:'Create '+entity.name,
                      request:{
                        method:'POST',
                        header:[
                          {key:'Authorization',value:'Bearer {{jwt_token}}',type:'text'},
                          {key:'Content-Type',value:'application/json',type:'text'}
                        ],
                        body:{mode:'raw',raw:JSON.stringify(body,null,2),options:{raw:{language:'json'}}},
                        url:{raw:'{{base_url}}'+path,host:['{{base_url}}'],path:[prefix,entity.name]}
                      }
                    });
                    // PUT
                    folder.item.push({
                      name:'Update '+entity.name,
                      request:{
                        method:'PUT',
                        header:[
                          {key:'Authorization',value:'Bearer {{jwt_token}}',type:'text'},
                          {key:'Content-Type',value:'application/json',type:'text'}
                        ],
                        body:{mode:'raw',raw:JSON.stringify(body,null,2),options:{raw:{language:'json'}}},
                        url:{raw:'{{base_url}}'+path+'/{{record_id}}',
                             host:['{{base_url}}'],path:[prefix,entity.name,'{{record_id}}']}
                      }
                    });
                  }
                  items.push(folder);
                }
                const collection = {
                  info:{
                    name:'DAS - '+proj.name,
                    schema:'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'
                  },
                  auth:{type:'bearer',bearer:[{key:'token',value:'{{jwt_token}}',type:'string'}]},
                  variable:[
                    {key:'base_url',value:baseUrl},
                    {key:'jwt_token',value:token},
                    {key:'record_id',value:''}
                  ],
                  item:items
                };
                const blob = new Blob([JSON.stringify(collection,null,2)],{type:'application/json'});
                const a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = 'DAS_'+proj.name+'_collection.json';
                a.click();
                URL.revokeObjectURL(a.href);
              };
            })();
            </script>
            """;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
