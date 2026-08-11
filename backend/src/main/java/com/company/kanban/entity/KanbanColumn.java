package com.company.kanban.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "kanban_columns")
public class KanbanColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer position;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    public KanbanColumn() {
    }

    public KanbanColumn(
            String name,
            Integer position,
            Board board) {

        this.name = name;
        this.position = position;
        this.board = board;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getPosition() {
        return position;
    }

    public Board getBoard() {
        return board;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public void setBoard(Board board) {
        this.board = board;
    }
}