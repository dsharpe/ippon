package com.ippon.poc.hostname;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.util.RequestUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import com.ippon.poc.hostname.HostNameProperties.Mapping;
import com.netflix.zuul.context.RequestContext;

/**
 * Based on {@link SimpleRouteLocator} but extended to use separate host name mappings
 * to map a host name to a Zuul route.
 */
public class HostNameRouteLocator implements RouteLocator {

	private static final Log log = LogFactory.getLog(HostNameRouteLocator.class);
	
	private ZuulProperties properties;
	
	private HostNameProperties hostNameProperties;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private String dispatcherServletPath = "/";
	private String zuulServletPath;

	private AtomicReference<Map<String, ZuulRoute>> routes = new AtomicReference<>();

	public HostNameRouteLocator(String servletPath, ZuulProperties properties, HostNameProperties hostNameProperties) {
		this.properties = properties;
		this.hostNameProperties = hostNameProperties;
		if (servletPath != null && StringUtils.hasText(servletPath)) {
			this.dispatcherServletPath = servletPath;
		}

		this.zuulServletPath = properties.getServletPath();
	}

	@Override
	public List<Route> getRoutes() {
		if (this.routes.get() == null) {
			this.routes.set(locateRoutes());
		}
		List<Route> values = new ArrayList<>();
		for (String url : this.routes.get().keySet()) {
			ZuulRoute route = this.routes.get().get(url);
			String path = route.getPath();
			values.add(getRoute(route, path));
		}
		return values;
	}

	@Override
	public Collection<String> getIgnoredPaths() {
		return this.properties.getIgnoredPatterns();
	}

	@Override
	public Route getMatchingRoute(final String path) {

		if (log.isDebugEnabled()) {
			log.debug("Finding route for path: " + path);
		}

		if (this.routes.get() == null) {
			this.routes.set(locateRoutes());
		}
		
	    RequestContext ctx = RequestContext.getCurrentContext();
	    HttpServletRequest request = ctx.getRequest();
	    String targetRoute = "";
	    for (Mapping hostNameMapping : hostNameProperties.getMappings()) {
	    	if (hostNameMapping.getHostName().equals(request.getServerName())) {
	    		targetRoute = hostNameMapping.getRoute();
	    		break;
	    	}
	    }

		log.debug("servletPath=" + this.dispatcherServletPath);
		log.debug("zuulServletPath=" + this.zuulServletPath);
		log.debug("RequestUtils.isDispatcherServletRequest()="
				+ RequestUtils.isDispatcherServletRequest());
		log.debug("RequestUtils.isZuulServletRequest()="
				+ RequestUtils.isZuulServletRequest());

		String adjustedPath = adjustPath(path);

		ZuulRoute route = null;
		if (!matchesIgnoredPatterns(adjustedPath)) {
			for (ZuulRoute zuulRoute : this.routes.get().values()) {
				if (zuulRoute.getId().equals(targetRoute)) {
					route = zuulRoute;
					break;
				}
			}
		}
		log.debug("route matched=" + route);

		return getRoute(route, adjustedPath);

	}

	private Route getRoute(ZuulRoute route, String path) {
		if (route == null) {
			return null;
		}
		String targetPath = path;
		String prefix = this.properties.getPrefix();
		if (path.startsWith(prefix) && this.properties.isStripPrefix()) {
			targetPath = path.substring(prefix.length());
		}
		if (route.isStripPrefix()) {
			int index = route.getPath().indexOf("*") - 1;
			if (index > 0) {
				String routePrefix = route.getPath().substring(0, index);
				targetPath = targetPath.replaceFirst(routePrefix, "");
				prefix = prefix + routePrefix;
			}
		}
		Boolean retryable = this.properties.getRetryable();
		if (route.getRetryable() != null) {
			retryable = route.getRetryable();
		}
		return new Route(route.getId(), targetPath, route.getLocation(), prefix,
				retryable,
				route.isCustomSensitiveHeaders() ? route.getSensitiveHeaders() : null);
	}

	/**
	 * Calculate all the routes and set up a cache for the values. Subclasses can call
	 * this method if they need to implement {@link RefreshableRouteLocator}.
	 */
	protected void doRefresh() {
		this.routes.set(locateRoutes());
	}

	/**
	 * Compute a map of path pattern to route. The default is just a static map from the
	 * {@link ZuulProperties}, but subclasses can add dynamic calculations.
	 */
	protected Map<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<String, ZuulRoute>();
		for (ZuulRoute route : this.properties.getRoutes().values()) {
			routesMap.put(route.getPath(), route);
		}
		return routesMap;
	}

	protected boolean matchesIgnoredPatterns(String path) {
		for (String pattern : this.properties.getIgnoredPatterns()) {
			log.debug("Matching ignored pattern:" + pattern);
			if (this.pathMatcher.match(pattern, path)) {
				log.debug("Path " + path + " matches ignored pattern " + pattern);
				return true;
			}
		}
		return false;
	}

	private String adjustPath(final String path) {
		String adjustedPath = path;

		if (RequestUtils.isDispatcherServletRequest()
				&& StringUtils.hasText(this.dispatcherServletPath)) {
			if (!this.dispatcherServletPath.equals("/")) {
				adjustedPath = path.substring(this.dispatcherServletPath.length());
				log.debug("Stripped dispatcherServletPath");
			}
		}
		else if (RequestUtils.isZuulServletRequest()) {
			if (StringUtils.hasText(this.zuulServletPath)
					&& !this.zuulServletPath.equals("/")) {
				adjustedPath = path.substring(this.zuulServletPath.length());
				log.debug("Stripped zuulServletPath");
			}
		}
		else {
			// do nothing
		}

		log.debug("adjustedPath=" + path);
		return adjustedPath;
	}

}
