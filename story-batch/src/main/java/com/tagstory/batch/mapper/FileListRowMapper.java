package com.tagstory.batch.mapper;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FileListRowMapper implements RowMapper<List<String>> {

    @Override
    public List<String> mapRow(ResultSet rs, int rowNum) throws SQLException {
        List<String> result = new ArrayList<>();
        result.add(rs.getString("file_path"));
        return result;
    }
}
