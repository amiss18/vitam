/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Query Filter POJO
 */
public class QueryFilter {
    @JsonProperty("$offset")
    private int offset = 0;
    @JsonProperty("$limit")
    private int limit = 10000;
    @JsonProperty("$orderby")
    private Map<String, Integer> orderby;

    /**
     * Default constructor
     */
    public QueryFilter() {
        // nothing
    }

    /**
     * @return the offset filter
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @param offset
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @return the limit filter
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @return the map of order by filter
     */
    public Map<String, Integer> getOrderby() {
        return orderby;
    }

    /**
     * @param orderby
     */
    public void setOrderby(Map<String, Integer> orderby) {
        this.orderby = orderby;
    }
}
