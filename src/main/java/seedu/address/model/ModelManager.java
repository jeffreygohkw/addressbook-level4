package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PERSON;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TASK;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.ComponentManager;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.core.index.Index;
import seedu.address.commons.events.model.AddressBookChangedEvent;
import seedu.address.logic.parser.Prefix;
import seedu.address.model.person.NameContainsFavouritePredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.ReadOnlyPerson;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.model.tag.Tag;
import seedu.address.model.task.ReadOnlyTask;
import seedu.address.model.task.exceptions.DuplicateTaskException;
import seedu.address.model.task.exceptions.TaskNotFoundException;

/**
 * Represents the in-memory model of the address book data.
 * All changes to any model should be synchronized.
 */
public class ModelManager extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final AddressBook addressBook;
    private final FilteredList<ReadOnlyPerson> filteredPersons;
    private final FilteredList<ReadOnlyTask> filteredTasks;

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, UserPrefs userPrefs) {
        super();
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook + " and user prefs " + userPrefs);

        this.addressBook = new AddressBook(addressBook);
        filteredPersons = new FilteredList<>(this.addressBook.getPersonList());
        filteredTasks = new FilteredList<>(this.addressBook.getTasksList());
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    @Override
    public void resetData(ReadOnlyAddressBook newData) {
        addressBook.resetData(newData);
        indicateAddressBookChanged();
    }

    @Override
    public void resetPartialData(ReadOnlyAddressBook newData, Prefix type) {
        assert(type.equals(PREFIX_TASK) || type.equals(PREFIX_PERSON));
        if (type.equals(PREFIX_TASK)) {
            addressBook.resetPartialData(newData, PREFIX_TASK);
            indicateAddressBookChanged();
        } else {
            addressBook.resetPartialData(newData, PREFIX_PERSON);
            indicateAddressBookChanged();
        }
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    /** Raises an event to indicate the model has changed */
    private void indicateAddressBookChanged() {
        raise(new AddressBookChangedEvent(addressBook));
    }

    @Override
    public synchronized void deletePerson(ReadOnlyPerson target) throws PersonNotFoundException {
        addressBook.removePerson(target);
        indicateAddressBookChanged();
    }

    /**
     * Delete input tag from the specific persons shown in the last list.
     */
    @Override
    public synchronized void deleteTag(Tag toDelete, ArrayList<Index> personIndexes) throws PersonNotFoundException,
            DuplicatePersonException {
        for (int i = 0; i < personIndexes.size(); i++) {
            int index = personIndexes.get(i).getZeroBased();
            ReadOnlyPerson personWithTag = this.getFilteredPersonList().get(index);
            Person personWithoutTag = new Person(personWithTag);
            Set<Tag> newTags = new HashSet<Tag>(personWithoutTag.getTags());
            newTags.remove(toDelete);
            personWithoutTag.setTags(newTags);
            addressBook.updatePerson(personWithTag, personWithoutTag);
            indicateAddressBookChanged();
        }
    }

    @Override
    public synchronized void addPerson(ReadOnlyPerson person) throws DuplicatePersonException {
        addressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        indicateAddressBookChanged();
    }

    /**
     * Adds input tag to the specific persons shown in the last list.
     */
    @Override
    public synchronized void addTag(Tag toAdd, ArrayList<Index> personIndexes) throws PersonNotFoundException,
            DuplicatePersonException {
        for (int i = 0; i < personIndexes.size(); i++) {
            int index = personIndexes.get(i).getZeroBased();
            ReadOnlyPerson personWithoutTag = this.getFilteredPersonList().get(index);
            Person personWithTag = new Person(personWithoutTag);
            Set<Tag> newTags = new HashSet<Tag>(personWithTag.getTags());
            newTags.add(toAdd);
            personWithTag.setTags(newTags);
            addressBook.updatePerson(personWithoutTag, personWithTag);
            indicateAddressBookChanged();
        }
    }

    @Override
    public void updatePerson(ReadOnlyPerson target, ReadOnlyPerson editedPerson)
            throws DuplicatePersonException, PersonNotFoundException {
        requireAllNonNull(target, editedPerson);

        addressBook.updatePerson(target, editedPerson);
        indicateAddressBookChanged();
    }

    @Override
    public void sortPersons(String field, String order) {
        addressBook.sortPersonsBy(field, order);
        indicateAddressBookChanged();
    }

    /**
     * Replaces the toChange Tag with the newTag Tag, for all Person objects denoted by the indexes.
     * Guarantees: indexes contains at least 1 person that has the toChange Tag.
     */
    public synchronized void editTag(Tag toChange, Tag newTag, ArrayList<Index> indexes)
            throws PersonNotFoundException, DuplicatePersonException {
        List<ReadOnlyPerson> allPersons = this.getFilteredPersonList();
        Set<Tag> personTags;
        Person toUpdate;
        ReadOnlyPerson toRead;
        int index;
        for (Index i : indexes) {
            index = i.getZeroBased();
            toRead = allPersons.get(index);
            toUpdate = new Person(toRead);
            personTags = new HashSet<>(toRead.getTags());
            personTags.remove(toChange);
            personTags.add(newTag);
            toUpdate.setTags(personTags);
            addressBook.updatePerson(toRead, toUpdate);
        }
        indicateAddressBookChanged();
    }

    @Override
    public void favouritePerson(ReadOnlyPerson target) throws PersonNotFoundException {
        addressBook.favouritePerson(target);
        indicateAddressBookChanged();
    }

    @Override
    public synchronized void addTask(ReadOnlyTask toAdd) throws DuplicateTaskException {
        addressBook.addTask(toAdd);
        updateFilteredTaskList(PREDICATE_SHOW_ALL_TASKS);
        indicateAddressBookChanged();
    }

    @Override
    public void unfavouritePerson(ReadOnlyPerson target) throws PersonNotFoundException {
        addressBook.unfavouritePerson(target);
        updateFilteredPersonList(new NameContainsFavouritePredicate());
        indicateAddressBookChanged();
    }

    @Override
    public synchronized void deleteTask(ReadOnlyTask toDelete) throws TaskNotFoundException {
        addressBook.removeTask(toDelete);
        indicateAddressBookChanged();
    }

    @Override
    public void updateTask(ReadOnlyTask target, ReadOnlyTask editedTask)
            throws DuplicateTaskException, TaskNotFoundException {
        requireAllNonNull(target, editedTask);

        addressBook.updateTask(target, editedTask);
        indicateAddressBookChanged();
    }
    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code ReadOnlyPerson} backed by the internal list of
     * {@code addressBook}
     */
    @Override
    public ObservableList<ReadOnlyPerson> getFilteredPersonList() {
        return FXCollections.unmodifiableObservableList(filteredPersons);
    }

    @Override
    public ObservableList<ReadOnlyTask> getFilteredTaskList() {
        return FXCollections.unmodifiableObservableList(filteredTasks);
    }

    @Override
    public void updateFilteredPersonList(Predicate<ReadOnlyPerson> predicate) {
        requireNonNull(predicate);
        filteredPersons.setPredicate(predicate);
    }

    @Override
    public void updateFilteredTaskList(Predicate<ReadOnlyTask> predicate) {
        requireNonNull(predicate);
        filteredTasks.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object obj) {
        // short circuit if same object
        if (obj == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(obj instanceof ModelManager)) {
            return false;
        }

        // state check
        ModelManager other = (ModelManager) obj;
        return addressBook.equals(other.addressBook)
                && filteredPersons.equals(other.filteredPersons);
    }

}
