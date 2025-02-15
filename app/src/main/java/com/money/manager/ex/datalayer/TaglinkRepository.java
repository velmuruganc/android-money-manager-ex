package com.money.manager.ex.datalayer;

import android.content.Context;

import com.money.manager.ex.Constants;
import com.money.manager.ex.database.DatasetType;
import com.money.manager.ex.domainmodel.Attachment;
import com.money.manager.ex.domainmodel.RefType;
import com.money.manager.ex.domainmodel.Tag;
import com.money.manager.ex.domainmodel.Taglink;

import java.util.ArrayList;

public class TaglinkRepository extends RepositoryBase <Taglink> {

    private static final String TABLE_NAME = "TAGLINK_V1";
    private static final String ID_COLUMN = Taglink.TAGLINKID;
    private static final String NAME_COLUMN = "";

    public TaglinkRepository(Context context) {
        super(context, TABLE_NAME, DatasetType.TABLE, "taglink", ID_COLUMN, NAME_COLUMN);
    }

    @Override
    protected Taglink createEntity() {
        return new Taglink();
    }

    @Override
    public String[] getAllColumns() {
        return new String[] { ID_COLUMN +" AS _id",
                Taglink.TAGLINKID,
                Taglink.REFTYPE,
                Taglink.REFID,
                Taglink.TAGID
        };
    }

    public boolean deleteForType(Long refId, RefType refType) {
        if (refId == Constants.NOT_SET) return false;
        long result = delete(Taglink.REFID + "=? AND " + Taglink.REFTYPE + "=?", new String[] { Long.toString(refId), refType.getValue()});
        return result > 0;
    }

    public boolean save(ArrayList<Taglink> taglinks) {
        for (Taglink taglink : taglinks) {
            if ( ! save(taglink) )
                return false;
        }
        return true;
    }

    public boolean saveAllFor(RefType refType, long refId, ArrayList<Taglink> taglinks) {
        if ( taglinks == null || taglinks.size() == 0 ) {
            deleteForType(refId, refType);
            return true;
        }
        ArrayList<Taglink> old = loadByRef(refId, refType);
        for (Taglink entity : old) {
            if (!entity.inTaglinkList(taglinks))
                delete(entity.getId());
        }

        // be sure to set refid
        for ( int i = 0; i < taglinks.size(); i++ ) {
            taglinks.get(i).setRefId(refId);
            taglinks.get(i).setRefType(refType);
        }
        return save(taglinks);
    }

    public ArrayList<Taglink> loadByRef(long refId, RefType refType) {

        return new ArrayList<>(query(new Select(getAllColumns())
                .where(Attachment.REFID + "= ? AND " + Attachment.REFTYPE +  "= ?"
                        , Long.toString(refId), refType.getValue())
                .orderBy(ID_COLUMN)));
    }

    public String loadTagsfor(ArrayList<Taglink> listEntity) {
        if (listEntity == null) return "";
        String tags = "";
        TagRepository tagRepository = new TagRepository(getContext());
        for (Taglink entity : listEntity) {
            Tag tag = tagRepository.load(entity.getTagId());
            if (tag != null ) {
                if ( tags.isEmpty() )
                    tags = tag.getName();
                else
                    tags += ", " + tag.getName();
            }
        }
        return tags;
    }


}
