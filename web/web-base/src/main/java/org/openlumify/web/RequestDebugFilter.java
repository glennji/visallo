package org.openlumify.web;

import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestDebugFilter implements Filter {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(RequestDebugFilter.class);
    public static final String VISALLO_REQUEST_DEBUG = "openlumify.request.debug";

    public static final String HEADER_DELAY = "OpenLumify-Request-Delay-Millis";
    public static final String HEADER_ERROR = "OpenLumify-Request-Error";
    public static final String HEADER_ERROR_JSON = "OpenLumify-Request-Error-Json";

    static {
        if ("true".equals(System.getProperty(VISALLO_REQUEST_DEBUG))) {
            LOGGER.warn("Request debugging is enabled. Set -D%s=false to disable", VISALLO_REQUEST_DEBUG);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if ("true".equals(System.getProperty(VISALLO_REQUEST_DEBUG))) {
            if (processDebugCommands(request, response)) {
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean processDebugCommands(ServletRequest request, ServletResponse response) throws IOException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String delay = httpRequest.getHeader(HEADER_DELAY);
            String error = httpRequest.getHeader(HEADER_ERROR);
            String json = httpRequest.getHeader(HEADER_ERROR_JSON);

            if (delay != null) {
                try {
                    LOGGER.warn("OpenLumify Debug Header Found %s. Delaying for %s", HEADER_DELAY, delay);
                    Thread.sleep(Integer.parseInt(delay));
                } catch (InterruptedException e) { }
            }

            if (json != null) {
                LOGGER.warn("OpenLumify Debug Header Found %s. Sending error json instead: %s", HEADER_ERROR_JSON, json);
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.setContentType("application/json");
                httpResponse.setCharacterEncoding("UTF-8");
                httpResponse.getWriter().write(json);
                return true;
            }

            if (error != null) {
                LOGGER.warn("OpenLumify Debug Header Found %s. Sending error instead: %s", HEADER_ERROR, error);
                Integer code = Integer.parseInt(error);
                ((HttpServletResponse) response).sendError(code);
                return true;
            }
        }

        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
