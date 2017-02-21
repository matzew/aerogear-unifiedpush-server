/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.unifiedpush.rest.metrics;

import static org.jboss.aerogear.unifiedpush.rest.util.HttpRequestUtil.extractSortingQueryParamValue;
import com.qmino.miredot.annotations.ReturnType;
import org.jboss.aerogear.unifiedpush.message.cache.metrics.SimpleMetricsCache;
import org.jboss.aerogear.unifiedpush.service.metrics.PushMessageMetricsService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/metrics/messages")
public class PushMetricsEndpoint {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;

    @Inject
    private SimpleMetricsCache cache;

    @Inject
    private PushMessageMetricsService metricsService;

    /**
     * GET info about submitted push messages for the given Push Application
     *
     * @param id        id of {@link org.jboss.aerogear.unifiedpush.api.PushApplication}
     * @param page      page number
     * @param pageSize  number of items per page
     * @param sorting   sorting order: {@code asc} (default) or {@code desc}
     * @param search    search query
     * @return          list of PushMessageInformation objects
     *
     * @responseheader total            Total count of items
     * @responseheader receivers        Receivers
     * @responseheader appOpenedCounter App Opened Counter
     *
     * @statuscode 404 The requested PushApplication resource does not exist
     */
    @GET
    @Path("/application/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ReturnType("java.util.List<org.jboss.aerogear.unifiedpush.api.PushMessageInformation>")
    public Response pushMessageInformationPerApplication(
            @PathParam("id") String id,
            @QueryParam("page") Integer page,
            @QueryParam("per_page") Integer pageSize,
            @QueryParam("sort") String sorting,
            @QueryParam("search") String search) {

        pageSize = parsePageSize(pageSize);

        if (page == null) {
            page = 0;
        }

        if (id == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Could not find requested information").build();
        }

        return Response.ok(metricsService.findAllForPushApplication(id, search, extractSortingQueryParamValue(sorting), page, pageSize))
                .header("total", cache.getStore().get(id+":"+"total"))
                .header("receivers", cache.getStore().get(id+":"+"receivers"))
                .header("appOpenedCounter", cache.getStore().get(id+":"+"appOpenedCounter"))
                .build();
    }

    private Integer parsePageSize(Integer pageSize) {
        if (pageSize != null) {
            pageSize = Math.min(MAX_PAGE_SIZE, pageSize);
        } else {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }
}
