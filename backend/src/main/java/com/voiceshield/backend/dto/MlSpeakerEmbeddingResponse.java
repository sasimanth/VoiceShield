package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlSpeakerEmbeddingResponse {

    @JsonProperty("embedding")
    private List<Double> embedding;

    @JsonProperty("embedding_dim")
    private Integer embeddingDim;

    @JsonProperty("status")
    private String status;

    public MlSpeakerEmbeddingResponse() {}

    public MlSpeakerEmbeddingResponse(List<Double> embedding, Integer embeddingDim, String status) {
        this.embedding = embedding;
        this.embeddingDim = embeddingDim;
        this.status = status;
    }

    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }

    public Integer getEmbeddingDim() { return embeddingDim; }
    public void setEmbeddingDim(Integer embeddingDim) { this.embeddingDim = embeddingDim; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
